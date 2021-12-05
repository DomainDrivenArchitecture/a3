(ns asn1.parser
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]
            [clojure.pprint  :refer [pprint]])
  (:import java.io.RandomAccessFile
           java.nio.ByteBuffer
           javax.xml.bind.DatatypeConverter))

(def oid-encoding {"2A8648CE3D317" {:oid "1.2 .840.10045.3.1.7"
                                    :name "prime256v1"}})

(def tag-encoding {"00000" {:name "cont"
                            :header-length 2}
                   "00001" {:name "cont"
                            :header-length 2}
                   "00010" {:name "INTEGER"
                            :header-length 2}
                   "00011" {:name "BIT STRING"
                            :header-length 2}
                   "00100" {:name "OCTET STRING"
                            :header-length 2}
                   "00110" {:name "OBJECT"
                            :header-length 2}
                   "10000" {:name "SEQUENCE"
                            :header-length 2}})

(defn base64-extract
  [path]
  (reduce str "" (remove #(str/starts-with? % "----") (line-seq (io/reader path)))))

(defn base64-bytes
  [path]
  (let [b64-str ^String (base64-extract path)]
    (DatatypeConverter/parseBase64Binary b64-str)))

(defn base64-buffer
  [path]
  (ByteBuffer/wrap (base64-bytes path)))

(defn i->hex [b]
  (format "%X" b))

(defn i->bin [b]
  (take-last
   8
   (concat '(\0 \0 \0 \0 \0 \0 \0 \0)
           (char-array (Integer/toBinaryString b)))))

(defn read-tag [& bs]
  (if-let [byte (first bs)]
    (let [hex-string (i->hex byte)
          bin (i->bin byte)
          class (take 2 bin)
          primitive? (= \0 (last (take 3 bin)))
          tag (apply str (take-last 5 bin))
          elem (get tag-encoding tag)
          output (merge elem {:hex hex-string
                              :bin bin
                              :class class
                              :tag tag
                              :type (if primitive? :prim :cons)})]
      ;;(println output)
      output)
    (throw (ex-message "tag not found"))))

(defn read-length [b]
  (if (some? b)
    (do
      ;;(println "length" (i->hex b) (i->bin b))
      b)
    (throw (ex-message "length not found"))))

(defn decode-tag [state b]
  (let [elem (read-tag b)]
    (merge state 
           {:next-step :length
            :tag elem})))

(defn decode-length [state b]
  (let [length (read-length b)]
    (merge state
           {:next-step :value
            :value-length length})))

(defn decode-primitive-value [state bs]
  (let [{:keys [offset depth tag value-length result]} state
        {:keys [header-length type name]} tag
        hex-val (str/join (map #(format "%X" %) bs))
        value (if (= name "OBJECT") (get oid-encoding hex-val) hex-val)
        output  {:o offset
                 :d depth
                 :hl header-length
                 :l value-length
                 :v value
                 type name}]
    (merge state
           {:offset (+ offset value-length header-length)
            :next-step :tag
            :result (conj result output)})))

(defn decode-constructed-value [state bs]
  (let [{:keys [offset depth tag value-length result]} state
        {:keys [header-length type name]} tag
        output  {:o offset
                 :d depth
                 :hl header-length
                 :l value-length
                 type name}]
    (merge state
           {:offset (+ offset header-length)
            :next-step :tag
            :result (conj result output)})))

(defn descend [state]
  (let [{:keys [depth]} state]
    (merge state
           {:tag {}
            :next-step :tag
            :depth (inc depth)})))

(defn decode-elements [state & bs]
  (let [{:keys [next-step tag value-length]} state
        {:keys [type]} tag
        byte (first bs)]
    ;;(println next-step state bs)
    (case next-step
      :tag (if (some? bs) 
             (apply decode-elements
                    (decode-tag state byte)
                    (rest bs))
             (apply decode-elements
                    (merge state {:next-step :end})
                    bs))
      :length (apply decode-elements
                     (decode-length state byte)
                     (rest bs))
      :value (if (= type :prim)
               (apply decode-elements
                      (decode-primitive-value state (take value-length bs))
                      (nthrest bs value-length))
               (let [constructed-value (take value-length bs)
                     constructed-rest (nthrest bs value-length)
                     state-with-value (decode-constructed-value state constructed-value)
                     state-with-descend (merge (apply decode-elements
                                                      (descend state-with-value)
                                                      constructed-value)
                                               (select-keys state-with-value [:depth :next-step]))]
                 (apply decode-elements
                        state-with-descend
                        constructed-rest)))
      :end state
      (throw (ex-message "unknown step")))))

(defn decode [& bs]
  (:result
   (apply decode-elements {:offset 0
                            :depth 0
                            :next-step :tag
                            :tag {}
                            :value-length 0
                            :result []}
          bs)))

(defn my-parse [bs]
  (->> bs
      (map )))

(defn parse-asn1
  [bb]
  ::nothing-parsed)

(defn -main [& args]
  (if-let [key-path (first args)]
    (pprint (parse-asn1 (base64-buffer key-path)))
    (binding [*out* *err*]
      (println "no path given")
      (System/exit 1))))