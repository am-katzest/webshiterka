(ns  todoot.core)

(defrecord todo [title description place dueDate])

(def todos (atom []))

(defn append-todo! [new] (swap! todos conj new))
(append-todo! (->todo "Learn JS" "Create a demo application for my TODO's" "445" (js/Date. 2019 10 16)))
(append-todo! (->todo "Lecture Test" "Quick test from the first three lectures" "F6" (js/Date. 2019 10 17)))

(defn read-todo []
  (let [[title desc place date]
        (map #(.-value (.getElementById js/document %))
             ["inputTitle" "inputDescription" "inputPlace" "inputDate"])]
    (->todo title desc place (js/Date. date))))

(defn make-deleter [id]
  (let [new-button (.createElement js/document "input")]
    (set! (.-type new-button) "button")
    (.setAttribute new-button "value" "unadd")
    (.setAttribute new-button "onclick" (str "todoot.core.deleter(" id ")")) ;ihateitihateitihateit
    new-button))

(defn htmlize-todo [td]
  (let [ret (.createElement js/document "div")
        deleter (make-deleter (str "\"" (:title td) "\"")) ; good enough, there should be id's but uhh
        content (.createTextNode js/document (str (:title td) " " (:description td) (into {} td)))]
    (.appendChild ret deleter)
    (.appendChild ret content)
    ret))

(defn kill-all-children [elem]
  (when-let [child (.-lastChild elem)]
    (.removeChild elem child)
    (recur elem)))

(defn update-todo-list []
  (let [root (.getElementById js/document "todoListView")]
    (kill-all-children root)
    (doseq [td @todos]
      (.appendChild root (htmlize-todo td)))))

(defn add-todo []
  (append-todo! (read-todo))
  (update-todo-list))

(defn deleter [id]
  (swap! todos (fn [lst] (remove #(= (:title %) id) lst)))
  (update-todo-list))

(defn init [] (update-todo-list))
