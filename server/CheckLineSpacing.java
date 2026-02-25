import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.EscPosConst;

public class CheckLineSpacing {
    public static void main(String[] args) throws Exception {
        Style style = new Style().setFontSize(Style.FontSize._2, Style.FontSize._2);
        byte[] bytes = style.getConfigBytes();
        for (byte b : bytes) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }
}
