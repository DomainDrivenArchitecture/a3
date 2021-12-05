(ns asn1.parser
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]
            [clojure.pprint  :refer [pprint]])
  (:import java.nio.ByteBuffer
           javax.xml.bind.DatatypeConverter))

(def oid-encoding {"2A8648CE3D030107" {:oid "1.2 .840.10045.3.1.7"
                                       :name "prime256v1"}})

(def tag-encoding {"00000" {:name "cont [ 0 ]"
                            :t-header-length 1}
                   "00001" {:name "cont [ 1 ]"
                            :t-header-length 1}
                   "00010" {:name "INTEGER"
                            :t-header-length 1}
                   "00011" {:name "BIT STRING"
                            :t-header-length 1}
                   "00100" {:name "OCTET STRING"
                            :t-header-length 1}
                   "00110" {:name "OBJECT"
                            :t-header-length 1}
                   "10000" {:name "SEQUENCE"
                            :t-header-length 1}})

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
  (format "%02X" (Byte/toUnsignedInt b)))

(defn i->bin [b]
  (take-last
   8
   (concat '(\0 \0 \0 \0 \0 \0 \0 \0)
           (char-array (Integer/toBinaryString b)))))

(defn read-tag [byte]
  (if (some? byte)
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
      output)
    (throw (ex-message "tag not found"))))

(defn read-length [byte]
  (if (some? byte)
    (let [hex-string (i->hex byte)
          bin (i->bin byte)
          short-form? (= \0 (first bin))
          length (bit-and 0x7F byte)
          output (merge {:hex hex-string
                         :bin bin
                         :short-form? short-form?}
                        (if short-form? 
                          {:l-header-length 1
                           :length length}
                          {:l-header-length (inc length)
                           :length-bytes length}))]
      output)
    (throw (ex-message "length not found"))))

(defn decode-tag [state b]
  (let [elem (read-tag b)]
    (merge state 
           {:next-step :length
            :tag elem})))

(defn decode-length [state byte]
  (let [elem (read-length byte)
        {:keys [short-form? ]} elem]
    (merge state
           {:value-length elem
            :next-step (if short-form? :value :length-long)})))

(defn decode-length-long [state bytes]
  (let [elem (:value-length state)
        length (BigInteger. (str/join (map i->hex bytes)) 16)]
    (merge state
           {:value-length (merge elem
                                 {:length length})
            :next-step :value})))

(defn decode-primitive-value [state bs]
  (let [{:keys [offset depth tag value-length result]} state
        {:keys [t-header-length type name]} tag
        {:keys [l-header-length length]} value-length
        hex-val (str/join (map i->hex bs))
        value (if (= name "OBJECT") (get oid-encoding hex-val) hex-val)
        output  {:o offset
                 :d depth
                 :hl (+ t-header-length l-header-length)
                 :l length
                 :v value
                 type name}]
    (merge state
           {:offset (+ offset t-header-length l-header-length length)
            :next-step :tag
            :result (conj result output)})))

(defn decode-constructed-value [state bs]
  (let [{:keys [offset depth tag value-length result]} state
        {:keys [t-header-length type name]} tag
        {:keys [l-header-length length]} value-length
        output  {:o offset
                 :d depth
                 :hl (+ t-header-length l-header-length)
                 :l length
                 type name}]
    (merge state
           {:offset (+ offset t-header-length l-header-length)
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
        {:keys [length length-bytes]} value-length
        byte (first bs)]
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
      :length-long (apply decode-elements
                          (decode-length-long state (take length-bytes bs))
                          (nthrest bs length-bytes))
      :value (if (= type :prim)
               (apply decode-elements
                      (decode-primitive-value state (take length bs))
                      (nthrest bs length))
               (let [constructed-value (take length bs)
                     constructed-rest (nthrest bs length)
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
                           :header-length 0
                           :result []}
          bs)))

(defn parse-asn1
  [bs]
  (apply decode bs))

(defn -main [& args]
  (if-let [key-path (first args)]
    (pprint (parse-asn1 (base64-bytes key-path)))
    (binding [*out* *err*]
      (println "no path given")
      (System/exit 1))))