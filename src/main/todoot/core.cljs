(ns  todoot.core
  (:require
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   [ajax.core :as ax]))

(def $ (js* "$")) ;; jquery is imported via html

;; code is a little mangled

(declare delete-todo!)

;; utils
(defn v$ [x] (.val ($ x)))

(defn parse-date  [date] (if (= date "") nil (js/Date. date)))

(defn d$ [x] (parse-date (v$ x)))

(defn append! [root children]
  (doseq [c children]
    (.append root c))
  root)

;; data

(defrecord todo [title description place dueDate])

(defonce todos (atom #{}))

(defn read-todo []
  (->todo (v$ "#inputTitle")
          (v$ "#inputDescription")
          (v$ "#inputPlace")
          (d$ "#inputDate")))

;; displaying

;; ;; table

(defn make-delete-btn [item]
  (doto ($ "<input>")
    (.attr "type" "button")
    (.attr "value" "⌦")
    (.attr "class" "btn")
    (.on "click" #(delete-todo! item))))

(defn into-table-cell [child]
  (-> ($ "<td>")
      (.append child)))

(defn into-table-row [nodes]
  (append! ($ "<tr>") nodes))

(defn todo->html [td]
  (->> td
       ((juxt :title
              #(->> %
                    :dueDate
                    t/date-time
                    (tf/unparse (tf/formatter "yyyy-MM-dd")))
              :place
              :description))
       (cons (make-delete-btn td))
       (map into-table-cell)
       into-table-row))

(defn avialable? [item]
  (let [Search (v$ "#inputSearch")
        search (.toLowerCase Search)
        matches #(.includes (.toLowerCase %) search)
        lower-bound (d$ "#inputAfter")
        upper-bound (d$ "#inputBefore")
        date (:dueDate item)]
    (and (or (= search "")
             (matches (:description item))
             (matches (:title item)))
         (or (nil? lower-bound)
             (>= date lower-bound))
         (or (nil? upper-bound)
             (<= date upper-bound)))))

(defn redraw-todos []
  (let [root ($ "#todoListView")]
    (.empty root)
    (->> @todos
         (filter avialable?)
         (map todo->html)
         (append! root))))

;; json handling
(defn todos->json [x]
  (->> x
       clj->js
       (.stringify js/JSON)))

(defn fix-todo [x]
  (update (into {} (map (fn [[k v]] [(keyword k) v]) x))
          :dueDate parse-date))

(defn json->todos [new]
  (->> new
       js->clj
       (map fix-todo)
       (into #{})))

;; bin <-> todos atom

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

(defn save! [] (-> @todos todos->json send-todos-to-api))

(defn load! [] (get-todos-from-api
                #(do (reset! todos (json->todos %))
                     (redraw-todos))))

;; modifying of todo list

(defn add-todo! [item]
  (swap! todos conj item)
  (redraw-todos)
  (save!))

(defn delete-todo! [item]
  (swap! todos disj item)
  (redraw-todos)
  (save!))

;; is called at the beginning
(defn init []
  (load!)
  ($ (fn []
       (.on ($ "#addTodo") "click" #(add-todo! (read-todo)))
       (.on ($ "#inputSearch") "keyup" redraw-todos)
       (.on ($ "#inputBefore") "change" redraw-todos)
       (.on ($ "#inputAfter") "change" redraw-todos)
       (redraw-todos))))
