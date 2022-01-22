(ns tsunbot.http
  (:import java.net.http.HttpRequest
           java.net.http.HttpRequest$Builder
           java.net.http.HttpRequest$BodyPublishers
           java.net.http.HttpResponse$BodyHandlers
           java.net.http.HttpClient
           java.net.URI))

(defn add-headers [^HttpRequest$Builder req headers]
  (doseq [[k v] headers] (.header req k v))
  req)

(defn get-req
  ([url] (get-req url {}))
  ([url headers]
   (let [c   (HttpClient/newHttpClient)
         req (-> (HttpRequest/newBuilder)
                 (add-headers headers)
                 (.uri (URI/create url))
                 (.GET)
                 (.build))]
     (.send c req (HttpResponse$BodyHandlers/ofString)))))

(defn post-req
  ([url] (post-req url {} ""))
  ([url headers body]
   (let [c   (HttpClient/newHttpClient)
         req (-> (HttpRequest/newBuilder)
                 (add-headers headers)
                 (.uri (URI/create url))
                 (.POST (HttpRequest$BodyPublishers/ofString body))
                 (.build))]
     (.send c req (HttpResponse$BodyHandlers/ofString)))))
