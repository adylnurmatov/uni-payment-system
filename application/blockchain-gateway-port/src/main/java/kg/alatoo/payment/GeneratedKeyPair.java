package kg.alatoo.payment;

/**
 * Новая пара ключей. privateKeyHex живёт только в памяти до шифрования.
 */
public record GeneratedKeyPair(String address, String privateKeyHex) {

    @Override
    public String toString() {
        return "GeneratedKeyPair[address=" + address + ", privateKeyHex=***]";
    }
}
