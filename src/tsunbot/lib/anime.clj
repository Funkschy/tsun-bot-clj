(ns tsunbot.lib.anime
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [tsunbot.http :as http])
  (:import java.time.LocalDate
           java.net.URLEncoder))

(def fetch-anilist-airing
  (memoize
    (fn [year season]
      (let
        [airing-eps-query
         (str/join \space
                   ["query media("
                    "$page: Int = 1,"
                    "$type: MediaType,"
                    "$season: MediaSeason,"
                    "$year: String,"
                    "$sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC]) {"
                    "Page(page: $page, perPage: 100) {"
                    "media(type: $type, season: $season, startDate_like: $year, sort: $sort) {"
                    "mal_id:idMal"
                    "episodes"
                    "nextAiringEpisode { airingAt timeUntilAiring episode }"
                    "}}}"])
         params {"query" airing-eps-query
                 "variables" {"season" (str/upper-case season)
                              "type"   "ANIME"
                              "year"   (str year \%)}}]
        (try
          (log/info "fetching airing anime for" season year)
          (let [res (http/post-req "https://graphql.anilist.co"
                                   {"Content-Type" "application/json"}
                                   (json/write-str params))]
            (when (not= 200 (.statusCode res))
              (log/error (str "Could not get anilist data " (.statusCode res) " " (.body res))))
            (get-in (-> res .body json/read-str)
                    ["data" "Page" "media"]))
          (catch Exception e
            (log/error e)
            nil))))))

(defn fetch-mal-watching [username]
  (log/info "Fetching watching list of" username)
  (letfn [(animelist-url [username]
            (str "https://api.jikan.moe/v3/user/"
                 (URLEncoder/encode username "UTF-8")
                 "/animelist/watching"))]
    (try
      (get (-> username
               (animelist-url)
               (http/get-req)
               (.body)
               (json/read-str))
           "anime")
      (catch Exception e
        (log/error "could not fetch MAL watching" e)
        nil))))

(defn current-episode [anime-info]
  (if (anime-info "nextAiringEpisode")
    (dec (get-in anime-info ["nextAiringEpisode" "episode"]))
    (anime-info "episodes")))

(defn behind-schedule [anime-info]
  (let [watched (anime-info "watched_episodes")
        current (current-episode anime-info)]
    (if (and current watched )
      (- current watched)
      0)))

(defn get-year-and-season []
  (let [seasons (into [] (mapcat (partial repeat 3) ["WINTER" "SPRING" "SUMMER" "FALL"]))
        date    (LocalDate/now)]
    [(.getYear date)
     (-> date (.getMonthValue) (dec) (seasons))]))

(defn fetch-behind-schedule [mal-username]
  (log/info "Fetching behind schedule anime for" mal-username)
  (try
    (let
      [mal-data (fetch-mal-watching mal-username)
       ani-data (apply fetch-anilist-airing (get-year-and-season))]
      (when (nil? mal-data)
        (log/error "could not fetch mal data for" mal-username))
      (when (nil? ani-data)
        (log/error "could not currently running shows for" (get-year-and-season)))
      (log/info mal-username "is watching" (count mal-data) "shows")
      (when (and mal-data ani-data)
        (->> (concat mal-data ani-data)
             (group-by #(get % "mal_id"))
             (vals)
             (filter #(= 2 (count %)))
             (map (partial apply merge))
             (map #(conj % [:behind (behind-schedule %)]))
             (filter (comp not zero? :behind)))))
    (catch Exception e
      (log/info e)
      nil)))
