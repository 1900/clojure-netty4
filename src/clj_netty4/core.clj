(ns clj-netty4.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            )
  (:gen-class)
  (:import (java.net InetSocketAddress
                     InetAddress)
           (java.nio.charset Charset)
           (io.netty.bootstrap Bootstrap)
           (io.netty.channel ChannelOption
                             EventLoopGroup
                             ChannelHandlerContext
                             ChannelInitializer
                             ChannelHandler
                             SimpleChannelInboundHandler)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.channel.socket DatagramPacket)
           (io.netty.channel.socket.nio NioDatagramChannel)
           (io.netty.util CharsetUtil))
  )

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 12345
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn exit [status msg]
  (System/exit status))

(defn usage [options-summary]
  (->> ["Usage: -p [options] "
        ""
        "Options:"
        options-summary
        ""
        "Setting UDP Server port"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defonce udpServer (atom nil))

(defn receive-handler
  [content]
  (let [text (.toString content (Charset/forName "UTF-8"))
        ]
    (prn (format "received: %s" text))
    text))

(defn make-udp-handler
  [handle]
  (proxy [SimpleChannelInboundHandler] []
    (channelRead0 [context packet]
      (let [content (.content packet)]
        (handle content)))

    (channelReadComplete [context]
      (.flush context))

    ;(exceptionCaught [context exception-event]
    ;  (warn (.getCause exception-event) "UDP handler caught"))
    )
  )

(defn udp-server
  ([] (udp-server {}))
  ([opts]
   (let [opts (merge {:addr   (InetAddress/getByName "0.0.0.0")
                      :port   12345
                      :handle receive-handler}
                     opts)
         group (NioEventLoopGroup.)
         bootstrap (Bootstrap.)
         handler (make-udp-handler (:handle opts))
         addr (:addr opts)
         port (:port opts)]
     (reset! udpServer
             (doto bootstrap
               (.group group)
               (.channel NioDatagramChannel)
               (.option ChannelOption/SO_BROADCAST true)
               (.handler handler)))

     (let [f (-> bootstrap
                 (.bind addr port)
                 (.sync))]
       (-> f
           (.channel)
           (.closeFuture)
           (.sync))))))

(defn stop []
  (when @udpServer
    (@udpServer)
    (reset! udpServer nil)))

(defn -main
  [& args-command]
  (let [{:keys [options arguments errors summary]} (parse-opts args-command cli-options :in-order true)]
    (cond (:help options)
          (exit 0 (usage summary))
          :else
          (udp-server options))))