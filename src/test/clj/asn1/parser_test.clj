(ns asn1.parser-test
  (:require
   [clojure.test :refer :all]
   [data-test :refer :all]
   [clojure.spec.test.alpha :as stest]
   [asn1.parser :as cut]))

(deftest should-accept-empty-input
  (is (= []
         (cut/decode))
      (= []
         (apply cut/decode []))))

(deftest should-parse-rsa
  (is
   (= "    
    0:d=0  hl=4 l=1188 cons: SEQUENCE          
    4:d=1  hl=2 l=   1 prim: INTEGER           :00
    7:d=1  hl=4 l= 257 prim: INTEGER           :CFDC2D8940865B6762869EBAD861763B17C20B0882D24690F7CCA5A2EAE4F171B3D0CEC02EB524EB88FAF5371C2A25BE02DA9363F0B5FCD4368C8DBB700EEE50290926EB6597E8E6E8EC50B591758CCCA1C4965AC0E8B0F87D3C7578D3AB369D8C01D4C7D3590B4AE98D0189E404C3A36BB50014C8D7155B251F857C99EA1C7A6FA3D58E6721DFD32569419B635A838666658F62FDDA4BF99B25AF6F876396F3D7D164DE58AB163573849BF94BDD2EE7435117229A1E67C44AD9F8E39F459B30C36CD057BC1E98C7B02404153175378FD6675A287F9F0DDF3AC9608AF497EBDB4EA224F4432B8192E7B968E5DE0275A401767D0102249601528128CCC0F47553
  268:d=1  hl=2 l=   3 prim: INTEGER           :010001
  273:d=1  hl=4 l= 257 prim: INTEGER           :8F2485875B942B260B47C1CFD26B6BCC4E14271CF95A6D962FD012D11B0A06EBD2751DAC637E03AAE06E52A85D10F964DCFA413E35EA5DEE96923C98B416D42F07D371A5F214A7233D8C601869CF95E2E61447A6BFEA7ED4E109DDAC13B878A80E5EC475C82A2E08ACC59A25A78D505D1E334B9704A879E086B42A6D206184B1DD584D9353E6FEAF96BFAC9646915BB68FC0A88FFEBBA03C5F2E55601A70D7B9700486FDF4D7E1AEA02060035C45F887BAA7A776B2FC6B4784AEEC49D7A337ABA9FD4AB29EA64035EA6B44731235570DBE13C436A2F530F7E727F3E6E754F28C83169ED0448A06D4537C3E94962615FCA3EFAB923FD8D64A6FAC0670DCBB4DA1
  534:d=1  hl=3 l= 129 prim: INTEGER           :FCE5366F3A1F6DBFF0329C9032CD59CA89F7A173AE9AC3893F333D16B18D36D3EDB35F4182B066C4CA46D63249D5936C0CA39A63B7F4A4D060AA0C4C2F443715DB5000C554D411CF1FB45BD4766ADF545461A9004C269259014BC3D85688FD2A3BF3BC4116FF5B547443FEFF45865AAD5C35B9C9F587DE811EC460DDE7993E07
  666:d=1  hl=3 l= 129 prim: INTEGER           :D2696E34D5DEE094188FB9B99AD42AAA1F0C7A777E31A16595CFB4E718AC8082F9E5CDDFD660913A0FA476526E22A019A7CEFCA723093670AE8BF1FCB8BD883B7A8ABC322BFA25854FD5781DC075F39109FD25E0D94E369C929189D3B3CB94AD17D62C092AE38C54C38C2E84D8E8094025A02660B5A6819E583AE5AC5A2EFB55
  798:d=1  hl=3 l= 129 prim: INTEGER           :8685B2AFF5BDF4164E41CDD05285B346AE9F1FADB66A32AB16083E6D8CE2AE108B7787AB0446AB53F0B93F851E8B5250FE642462F8DE54B15749FD22A55DE6E36476A9024EBE43FEE6417D3B86061167FD3EB9B423CDBCB9459C34C0263FD65319F2EBE7BFC0F3A6F7F59775D858C071490477207BCFDB46D9C3E23707D4170F
  930:d=1  hl=3 l= 128 prim: INTEGER           :6C612C080105AD4DFB1CC5008A74B0893236FE39E08175EDDA6DC373A9ACE9010DF145CAFF247C89989710EF4295893BCDC8FB30A8064DC95ACEF0D548DF2F75DB97EC7A3756C0CEDB214C9E9E8CE2E99968908331CB6CABD77F29AC27173CBCAFE37C8938533EEC46514580DE2D1EDEB0C2BE21E04F24C2C8158649F1A5F385
 1061:d=1  hl=3 l= 128 prim: INTEGER           :35E77E655E16F8060A4024D6EA70ABBE57B16BB73CCAC022BA2CF03281F4CE0C23058F863D22C3D376E7723ACA5912B04C9235A2C2F1E24D0F3449D03D0522C7264003A52C0FF004219E9036201A00C94562193728858497133E8B5E98D31CAE1C0F1208324230FF41CF35075E22F639DDFA29EED5066DF635E064B8135ED394
"
            (cut/parse-asn1 "src/test/resources/example-rsa.pem"))))

(deftest should-parse-ec
  (is
   (= "
    0:d=0  hl=2 l= 119 cons: SEQUENCE          
    2:d=1  hl=2 l=   1 prim: INTEGER           :01
    5:d=1  hl=2 l=  32 prim: OCTET STRING      [HEX DUMP]:03124552AFEF2CA9D3937418461E9416E04C7CF36252728F6BF38D797A5B4E3A
   39:d=1  hl=2 l=  10 cons: cont [ 0 ]        
   41:d=2  hl=2 l=   8 prim: OBJECT            :prime256v1
   51:d=1  hl=2 l=  68 cons: cont [ 1 ]        
   53:d=2  hl=2 l=  66 prim: BIT STRING"
      (cut/parse-asn1 "src/test/resources/example-ec.pem"))))