(ns hello
  (:require [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.content-negotiation :as conneg]))

(def unmentionables #{"YHWH" "Voldemort" "Mxyzptlk" "Rumplestiltskin" "曹操"})

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn ok [body]
  {:status 200
   :body body
   :headers {"Content-Type" "text/html"}})

(defn not-found []
  {:status 404 :body "Not found\n"})

(defn bad-request []
  {:status 400 :body "Bad Request\n"})

(defn greeting-for [nm]
  (cond
    (nil? nm) (ok "Hello, world!\n")
    (empty? nm) (bad-request)
    (unmentionables nm) (not-found)
    :else (ok (str "Hello, " nm "\n"))))

(defn respond-hello [request]
  (let [nm (get-in request [:query-params :name])
        resp (greeting-for nm)]
    resp))

(def echo
  {:name ::echo
   :enter (fn [context]
            (let [request (:request context)
                  response (ok request)]
              (assoc context :response response)))})

(def routes
  (route/-expand-routes
   #{["/greet" :get [content-neg-intc respond-hello] :route-name :greet]
     ["/echo" :get echo]}))

(def service-map
  {::http/routes routes
   ::http/type :jetty
   ::http/port 8890})

(defn start []
  (http/start (http/create-server service-map)))

(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start
           (http/create-server
            (assoc service-map
                   ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(comment
  (respond-hello {:query-params {:name "jef"}})
  (restart))
