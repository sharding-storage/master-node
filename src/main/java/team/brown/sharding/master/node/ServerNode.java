package team.brown.sharding.master.node;

import team.brown.sharding.master.config.Constant;

import java.util.HashMap;

public class ServerNode {
    private final String address;
    private final HashMap<Integer, String> salts;

    public ServerNode(String address) {
        this.address = address;
        salts = new HashMap<>();
    }

    public String getAddress() {
        return address;
    }

    public HashMap<Integer, String> getSalts() {
        return salts;
    }

    public void addToSalts(int i, int j) {
        salts.put(i, Constant.BASE_SALT + j);
    }

    public String getSaltedByIdx(int i) {
        return salts.getOrDefault(i, "" + i);
    }

    @Override
    public String toString() {
        // TODO: будет использоваться для вычисления хеша
        // (hash(node.toString()))
        return address;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServerNode)) {
            return false;
        }
        return address.equals(((ServerNode)other).address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    public String baseToHash(int i) {
        return address + "-" + getSaltedByIdx(i);
    }
}