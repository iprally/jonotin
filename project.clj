(defproject jonotin "0.2.1"
  :description "Google Pub/Sub Java SDK wrapper"
  :url "https://github.com/iprally/jonotin"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.google.cloud/google-cloud-pubsub "1.102.1" :exclusions [io.grpc/grpc-api
                                                                              io.grpc/grpc-core
                                                                              io.grpc/grpc-netty-shaded]]
                 [io.grpc/grpc-netty-shaded "1.26.0" :exclusions [io.grpc/grpc-api io.grpc/grpc-core]]
                 [io.grpc/grpc-core "1.26.0" :exclusions [io.grpc/grpc-api]]
                 [io.grpc/grpc-api "1.26.0"]]
  :source-paths ["src"])
