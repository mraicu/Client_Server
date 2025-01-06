public class CountryScore {
    private int score;
    private String countryId;

    public CountryScore(int score, String countryId) {
        this.score = score;
        this.countryId = countryId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }
}
