(ns tsunbot.http
  (:import java.net.http.HttpRequest
           java.net.http.HttpRequest$BodyPublishers
           java.net.http.HttpResponse$BodyHandlers
           java.net.http.HttpClient
           java.net.URI))

(defn get-req [url]
  (let [c   (HttpClient/newHttpClient)
        req (-> (HttpRequest/newBuilder)
                (.uri (URI/create url))
                (.GET)
                (.build))]
    (.send c req (HttpResponse$BodyHandlers/ofString))))

(defn post-req [url headers body]
  (let [add-headers (fn [r] (doseq [[k v] headers] (.header r k v)) r)
        c   (HttpClient/newHttpClient)
        req (-> (HttpRequest/newBuilder)
                (add-headers)
                (.uri (URI/create url))
                (.POST (HttpRequest$BodyPublishers/ofString body))
                (.build))]
    (.send c req (HttpResponse$BodyHandlers/ofString))))
