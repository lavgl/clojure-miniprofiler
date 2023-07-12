(ns clojure-miniprofiler.pedestal-example
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]

            [clojure-miniprofiler.core :as core]))

(defn dev-interceptors [service-map]
  (let [sm (-> service-map
               http/default-interceptors
               http/dev-interceptors
               (core/with-miniprofiler {}))]
    sm))


(defn another-slow []
  (core/custom-timing "sql" "query" "SELECT * FROM POSTS"
    (Thread/sleep 100)
    (do nil)))

(defn slow-fn []
  (println "slow-fn")
  (Thread/sleep 10)
  (core/trace "foo"
    (Thread/sleep 10)
    (core/trace "foo1" (Thread/sleep 10))
    (core/trace "foo2" (Thread/sleep 20)))
  (core/trace "Thread/sleep1"
    (Thread/sleep 100)
    (do nil)
    (another-slow)
    (core/trace "Thread/sleep2"
      (core/custom-timing "sql" "query" "SELECT * FROM USERS"
        (do nil))
      (do nil)))
  (core/trace "Thread/sleep3"
    (another-slow)
    (do nil))
  "<head><script src=\"/foo.js\"></script></head><body>lol</body>")


(defn respond-hello [request]          ;; <1>
  (slow-fn)
  {:status 200 :body "<!DOCTYPE html>
<html>
  <head>
    <title>Page Title</title>
  </head>
  <body>
    <h1>Hello, World!</h1>
  </body>
</html>"}) ;; <2>

(defn routes []
  (route/expand-routes                                   ;; <1>
    #{["/greet" :get [http/html-body #_core/log-interceptor respond-hello]
       :route-name :greet]})) ;; <2>

(defn create-server []
  (http/create-server     ;; <1>
    (-> {::http/routes (routes)  ;; <2>
         ::http/type   :jetty  ;; <3>
         ::http/port   3000
         ::http/join?  false
         ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}}
        dev-interceptors))) ;; <4>

(defn start []
  (http/start (create-server)))

(defonce server (atom nil))

(defn reload []
  (when @server
    (http/stop @server))
  (reset! server (create-server))
  (http/start @server))

(comment
  (reload))
