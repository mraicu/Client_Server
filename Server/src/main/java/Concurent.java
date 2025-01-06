public class Concurent {
    public String id;
    public int punctaj;
    public String countryId;

    public Concurent(String id, int punctaj, String countryId) {
        this.id = id;
        this.punctaj = punctaj;
        this.countryId = countryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPunctaj() {
        return punctaj;
    }

    @Override
    public String toString() {
        return "Concurent{" +
                "id='" + id + '\'' +
                ", punctaj=" + punctaj +
                '}';
    }

    public void setPunctaj(int punctaj) {
        this.punctaj = punctaj;
    }
}
