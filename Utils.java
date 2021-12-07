public class Utils {

    /**
     * int数值转为字节数组
     * @param i 32位有符号整数
     * @return  bytes数组长度是4,bytes[3]对应i的低8位,bytes[0]对应i的高8位...
     */
    public static byte[] int2Bytes(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) (i >> 24 & 0xFF);
        result[1] = (byte) (i >> 16 & 0xFF);
        result[2] = (byte) (i >> 8 & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * 字节数组转为int数值
     * 此处需要使用 类似0xFF来得到int值,类似于
     *      int num = bytes[3]是错误的
     * 因为byte是8个字节,范围只有-128-127,当0xff赋给byte的时候,byte显示的是-1
     * (byte)0xff = -1
     * (int)0xff = 255
     * @param bytes
     * @return
     */
    public static int bytes2Int(byte[] bytes){
        int num = bytes[3] & 0xFF;
        num |= ((bytes[2] << 8) & 0xFF00);
        num |= ((bytes[1] << 16) & 0xFF0000);
        num |= ((bytes[0] << 24)  & 0xFF000000);
        return num;
    }

    public static void main(String[] args) {
        System.out.println(bytes2Int(int2Bytes(Integer.MIN_VALUE)));
        System.out.println(bytes2Int(int2Bytes(Integer.MAX_VALUE)));
    }

}
