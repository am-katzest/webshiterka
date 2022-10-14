(ns  todoot.core
  (:require
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   [cljs-time.coerce :as tt]
   [todoot.bin :as bin]))

(def $ (js* "$")) ;; jquery is imported via html

;; code is a little mangled

(declare delete-todo!)

;; utils
(defn v$ [x] (.val ($ x)))

(defn parse-date  [date] (if (= date "") nil (t/date-time (js/Date. date))))

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
              #(tf/unparse (tf/formatter "yyyy-MM-dd") (:dueDate %))
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
             (t/after? date lower-bound))
         (or (nil? upper-bound)
             (t/before? date upper-bound)))))

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
       (map #(update % :dueDate tt/to-date))
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
(defn save! [] (-> @todos todos->json bin/send-todos-to-api))

(defn load! [] (bin/get-todos-from-api
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
