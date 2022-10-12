(ns todoot.bin
  (:require [ajax.core :as ax]))

(def api-key "$2b$10$aTUTTwvgslmXXpwWAxJ4j.hyTIPO4yMsSeE6vtZzRhBafUjQXKMA6")

(def bin-url (str "https://api.jsonbin.io/v3/b/" "634471970e6a79321e23f901"))

(defn get-todos-from-api [cb]
  (ax/GET bin-url
    {:headers {:X-Master-Key api-key}
     :handler #(cb (get % "record"))
     :error-handler #(js/alert %)}))

(defn send-todos-to-api [data]
  (ax/PUT bin-url
    {:headers {:X-Master-Key api-key
               :Content-Type "application/json"}
     :body data
     :error-handler #(js/alert %)}))
