(ns  todoot.core
  (:require [ajax.core :as ax]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tt]))

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

(defrecord todo [title description place dueDate])

(defonce todos (atom #{}))

(defn append-todo! [new] (swap! todos conj new))

(defn get-html-value [name] (.-value (.getElementById js/document name)))
(defn parse-date  [date] (if (= date "") nil (t/date-time (js/Date. date))))
(defn read-todo []
  (let [[title desc place date]
        (map get-html-value
             ["inputTitle" "inputDescription" "inputPlace" "inputDate"])]
    (->todo title desc place (parse-date date))))

(declare deleter)

(declare save)

(defn make-deleter [item]
  (let [new-button (.createElement js/document "input")]
    (set! (.-type new-button) "button")
    (doto new-button
      (.setAttribute  "value" "⌦")
      (.setAttribute  "class" "btn")
      (.addEventListener "click" #(deleter item)))
    new-button))

(defn make-table-entry [child type]
  (doto (.createElement js/document type)
    (.appendChild child)))

(defn text-nodize [text] (.createTextNode js/document text))
(defn make-row [nodes]
  (let [row (.createElement js/document "tr")]
    (doseq [n nodes]
      (.appendChild row n))
    row))
(defn htmlize-todo [td]
  (->> td
       ((juxt :title :dueDate :place :description))
       (apply (fn [a b c d] [a (tf/unparse (tf/formatter "yyyy-MM-dd") b) c d]))
       (map text-nodize)
       (cons (make-deleter td))
       (map #(make-table-entry % "td"))
       make-row))

(defn kill-all-children [elem]
  (when-let [child (.-lastChild elem)]
    (.removeChild elem child)
    (recur elem)))

(defn td-available [item search]
  (and (or (= search "")
           (.includes (:description item) search)
           (.includes (:title item) search))
       (if-let [lower-bound (parse-date (get-html-value "inputAfter"))]
         (t/after? (:dueDate  item) lower-bound) true)
       (if-let [upper-bound (parse-date (get-html-value "inputBefore"))]
         (t/before? (:dueDate  item) upper-bound) true)))

(defn update-todo-list []
  (let [root (.getElementById js/document "todoListView")
        search-str (get-html-value "inputSearch")]
    (kill-all-children root)
    (doseq [td (filter #(td-available % search-str) @todos)]
      (.appendChild root (htmlize-todo td)))))

(defn add-todo []
  (append-todo! (read-todo))
  (update-todo-list)
  (save))

(defn deleter [item]
  (swap! todos disj item)
  (update-todo-list)
  (save))

(defn get-todos [] (->> @todos
                        (map #(update % :dueDate tt/to-date))
                        clj->js
                        (.stringify js/JSON)))

(defn save [] (send-todos-to-api (get-todos)))

(defn recover [x]
  (update (into {} (map (fn [[k v]] [(keyword k) v]) x)) :dueDate #(tt/from-date (js/Date. %))))

(defn load-todos! [new]
  (->> new
       js->clj
       (map recover)
       (into #{})
       (reset! todos)))

(defn load [] (get-todos-from-api #(do (load-todos! %) (update-todo-list))))

(defn init [] (load)
  (.addEventListener js/window "load" update-todo-list))
