import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TestHash {
    public static void main(String[] args) throws Exception {
        String data = "vnp_Amount=12000000&vnp_Command=pay&vnp_CreateDate=20260615202902&vnp_CurrCode=VND&vnp_ExpireDate=20260615204402&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh%20toan%20don%20hang%2019&vnp_OrderType=other&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fpayment%2Fvnpay%2Freturn&vnp_TmnCode=NL605E75&vnp_TxnRef=19_1781530142575&vnp_Version=2.1.0";
        String key = "EZ5CFKOO34NTCPULG4K2UL367TCCQ8H2";
        
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(2 * result.length);
        for (byte b : result) {
            sb.append(String.format("%02x", b & 0xff));
        }
        System.out.println("Expected: 4578e1bfc716c5928ca71ec7d6c6af011a10027ef922e9dd0fd1706708376f6d52fda6846e98c8387ecbd5a6edb01ffb26bdb238945b103dbbe0d532c7884c73");
        System.out.println("Actual:   " + sb.toString());
    }
}
