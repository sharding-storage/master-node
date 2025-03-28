package team.brown.sharding.master.node;

public class ServerNode {
    private final String address;

    public ServerNode(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
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
}