(ns todoot.bin
  (:require [ajax.core :as ax]))

(def api-key "$2b$10$WeANikw9XvZVJZENB5lcOedfsqnvEEtJtPFzotoM7i3UhU/eYuP1S")

(def bin "634471970e6a79321e23f901")

(defn get-todos-from-api [cb]
  (ax/GET (str "https://api.jsonbin.io/v3/b/" bin)
    {:headers {:X-Access-Key api-key}
     :handler #(cb (get % "record"))
     :error-handler #(js/alert %)}))

(defn send-todos-to-api [json]
  (comment (ax/POST (str "https://api.jsonbin.io/v3/b/" bin)
             {:headers {:X-Access-Key api-key}
              :contentType "application/json"
              :data json
              :error-handler #(js/alert %)})))
