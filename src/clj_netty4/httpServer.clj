(ns clj-netty4.httpServer
  (:gen-class)
  (:import
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.channel Channel EventLoopGroup ChannelInitializer ChannelPipeline SimpleChannelInboundHandler]
    [io.netty.channel.nio NioEventLoopGroup]
    [io.netty.channel.socket SocketChannel]
    [io.netty.channel.socket.nio NioServerSocketChannel]
    [io.netty.handler.codec.http DefaultFullHttpResponse HttpContent LastHttpContent HttpHeaders HttpHeaders$Names HttpObject HttpRequest HttpRequestDecoder HttpResponseEncoder HttpResponseStatus HttpVersion]))

(defn start-netty-server
  [port handler]
  (let [boss-group (new NioEventLoopGroup 1)
        worker-group (new NioEventLoopGroup)]
    (try
      (let [b (new ServerBootstrap)]
        (-> b
            (.group boss-group worker-group)
            (.channel NioServerSocketChannel)
            (.childHandler handler))
        (-> b
            (.bind port)
            (.sync)
            (.channel)
            (.closeFuture)
            (.sync)))
      (finally
        (.shutdownGracefully boss-group)
        (.shutdownGracefully worker-group)))))

(defn create-initializer
  [handler-creator]
  (proxy [ChannelInitializer] []
    (initChannel [socket-channel]
      (doto (.pipeline socket-channel)
        (.addLast "decoder" (new HttpRequestDecoder))
        (.addLast "encoder" (new HttpResponseEncoder))
        (.addLast "handler" (handler-creator))))))

(defn create-handler
  []
  (proxy [SimpleChannelInboundHandler] []
    (channelReadComplete [channel-handler-context]
      (.flush channel-handler-context)
      (prn "channelReadComplete"))
    (channelRead0 [channel-handler-context message]
      (condp instance? message
        HttpRequest
        (do
          (prn "channelRead0 HttpRequest " (.name (.getMethod message)) "->" message))
        HttpContent
        (do
          (prn "channelRead0 HttpContent " message)
          (when (instance? LastHttpContent message)
            (prn "last message!")

            (.write channel-handler-context
                    (doto (new DefaultFullHttpResponse HttpVersion/HTTP_1_1 HttpResponseStatus/OK)
                      (-> .headers (.set HttpHeaders$Names/CONTENT_LENGTH 0))))))))
    (exceptionCaught [channel-handler-context cause]
      (.printStackTrace cause)
      (.close channel-handler-context))))

(defn -main
  [& args]
  (let [port 12345]
    (println "Starting http server at port" port)
    (start-netty-server port (create-initializer create-handler))))