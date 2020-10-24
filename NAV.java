package mixed;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NAV {
    final static Gson portfolioGson = new GsonBuilder().setPrettyPrinting().setLenient().registerTypeAdapter(PortfolioResponse.class, new PortfolioResponse.Deserializer()).create();
    final static Gson pricingGson = new GsonBuilder().setPrettyPrinting().setLenient().registerTypeAdapter(PricingResponse.class, new PricingResponse.Deserializer()).create();

    public static void main(String args[]) throws IOException {
        List<PortfolioResponse.Portfolio> portfolios = getHoldingService();
        List<PricingResponse.Pricing> pricings = getPricingService();
        
        Double value = 0.0;
        List<PortfolioResponse.Portfolio> portfolio = getPortfolioForDate(portfolios,"20190101");
        List<PricingResponse.Pricing> price = getPriceForDate(pricings, "20190101");
        
        for (PortfolioResponse.Portfolio p : portfolio) {
            for (PricingResponse.Pricing pr : price) {
                if (p.getSecurity().equals(pr.getSecurity())) {
                    value += p.getQuantity()*pr.getPrice();
                }
            }
        }
        System.out.println(value);
    }
    
    static List<PortfolioResponse.Portfolio> getPortfolioForDate(List<PortfolioResponse.Portfolio> portfolios, String date) {
        List<PortfolioResponse.Portfolio> res = new ArrayList<>();
        for (PortfolioResponse.Portfolio p : portfolios) {
            if (p.getDate().equals(date)) {
                res.add(p);
            }
        }
        return res;
    }

    static List<PricingResponse.Pricing> getPriceForDate(List<PricingResponse.Pricing> pricings, String date) {
        List<PricingResponse.Pricing> res = new ArrayList<>();
        for (PricingResponse.Pricing p : pricings) {
            if (p.getDate().equals(date)) {
                res.add(p);
            }
        }
        return res;
    }

    public static List<PricingResponse.Pricing> getPricingService() throws IOException {
        List<PricingResponse.Pricing> results = new ArrayList<>();
        String startUrl = "https://raw.githubusercontent.com/sood03/content/main/price1";
        PricingResponse response = getPricingResponse(startUrl);
        results.addAll(response.getData());
        int totalRecords = response.getTotalRecords();
        int perPage = response.getRecordsPerPage();
        String nextPageURL = response.getNextPage();
        for (int i = 1; i <= totalRecords/perPage; i++) {
            PricingResponse p = getPricingResponse(nextPageURL);
            results.addAll(p.getData());
            nextPageURL = p.getNextPage();
        }
        return results;
    }

    static PricingResponse getPricingResponse(String url) throws IOException {
        String serverReply = makeServerCall(url);
        PricingResponse response = pricingGson.fromJson(serverReply, PricingResponse.class);
        return response;
    }

    public static List<PortfolioResponse.Portfolio> getHoldingService() throws IOException {
        List<PortfolioResponse.Portfolio> results = new ArrayList<>();
        String startUrl = "https://raw.githubusercontent.com/sood03/content/main/portfolio1";
        PortfolioResponse response = getPortfolioResponse(startUrl);
        results.addAll(response.getData());

        int totalRecords = response.getTotalRecords();
        int perPage = response.getRecordsPerPage();
        String nextPageURL = response.getNextPage();
        for (int i = 1; i <= totalRecords/perPage; i++) {
            PortfolioResponse p = getPortfolioResponse(nextPageURL);
            results.addAll(p.getData());
            nextPageURL = p.getNextPage();
        }
        return results;
    }
    
    static PortfolioResponse getPortfolioResponse(String url) throws IOException {
        String serverReply = makeServerCall(url);
        PortfolioResponse response = portfolioGson.fromJson(serverReply, PortfolioResponse.class);
        return response;
    }

    static String makeServerCall(String getUrl) throws IOException {
        URL url = new URL(getUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String temp;
        StringBuffer output = new StringBuffer();

        while ((temp = br.readLine()) != null) {
            output.append(temp);
        }

        return output.toString();
    }
    
    static class PricingResponse {
        private int page;
        private String nextPage;
        private int recordsPerPage;
        private int totalRecords;
        private List<Pricing> data;

        public PricingResponse(int page, String nextPage, int recordsPerPage, int totalRecords, List<Pricing> data) {
            this.page = page;
            this.nextPage = nextPage;
            this.recordsPerPage = recordsPerPage;
            this.totalRecords = totalRecords;
            this.data = data;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public String getNextPage() {
            return nextPage;
        }

        public void setNextPage(String nextPage) {
            this.nextPage = nextPage;
        }

        public int getRecordsPerPage() {
            return recordsPerPage;
        }

        public void setRecordsPerPage(int recordsPerPage) {
            this.recordsPerPage = recordsPerPage;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }

        public List<Pricing> getData() {
            return data;
        }

        public void setData(List<Pricing> data) {
            this.data = data;
        }

        static final class Deserializer implements JsonDeserializer<PricingResponse> {

            /**
             * {@inheritDoc}
             */
            @Override
            public PricingResponse deserialize(final JsonElement json, final Type typeOfT,
                                                 final JsonDeserializationContext context) throws JsonParseException {
                final JsonObject jObject = json.getAsJsonObject();
                final int totalRecords = jObject.get("totalRecords").getAsInt();
                final int perPage = jObject.get("recordsPerPage").getAsInt();
                final int page = jObject.get("page").getAsInt();
                final String nextPage = jObject.get("nextPage").getAsString();
                final JsonArray jsonArray = jObject.get("data").getAsJsonArray();
                final List<Pricing> pricings = new ArrayList<>();

                for (int i = 0; i < jsonArray.size(); i++) {
                    final JsonObject pricingJson = jsonArray.get(i).getAsJsonObject();
                    final String date = pricingJson.get("date").getAsString();
                    final String security = pricingJson.get("security").getAsString();
                    final float price = pricingJson.get("price").getAsFloat();

                    final Pricing pricingInfo = new Pricing(date, security, price);
                    pricings.add(pricingInfo);
                }

                return new PricingResponse(page, nextPage, perPage, totalRecords, pricings);
            }
        }

        static class Pricing {
            String date;
            String security;
            float price;

            public Pricing(String date, String security, float price) {
                this.date = date;
                this.security = security;
                this.price = price;
            }

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public String getSecurity() {
                return security;
            }

            public void setSecurity(String security) {
                this.security = security;
            }

            public float getPrice() {
                return price;
            }

            public void setPrice(float price) {
                this.price = price;
            }
        }
    }

    static class PortfolioResponse {

        private int page;
        private String nextPage;
        private int recordsPerPage;
        private int totalRecords;
        private List<Portfolio> data;

        public PortfolioResponse(int page, int recordsPerPage, int totalRecords, String nextPage, List<Portfolio> data) {

            this.page = page;
            this.recordsPerPage = recordsPerPage;
            this.totalRecords = totalRecords;
            this.data = data;
            this.nextPage = nextPage;
        }

        public String getNextPage() {
            return nextPage;
        }

        public void setNextPage(String nextPage) {
            this.nextPage = nextPage;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getRecordsPerPage() {
            return recordsPerPage;
        }

        public void setRecordsPerPage(int recordsPerPage) {
            this.recordsPerPage = recordsPerPage;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }

        public List<Portfolio> getData() {
            return data;
        }

        public void setData(List<Portfolio> data) {
            this.data = data;
        }

        static final class Deserializer implements JsonDeserializer<PortfolioResponse> {

            /**
             * {@inheritDoc}
             */
            @Override
            public PortfolioResponse deserialize(final JsonElement json, final Type typeOfT,
                                                 final JsonDeserializationContext context) throws JsonParseException {
                final JsonObject jObject = json.getAsJsonObject();
                final int totalRecords = jObject.get("totalRecords").getAsInt();
                final int perPage = jObject.get("recordsPerPage").getAsInt();
                final int page = jObject.get("page").getAsInt();
                final String nextPage = jObject.get("nextPage").getAsString();
                final JsonArray jsonArray = jObject.get("data").getAsJsonArray();
                final List<Portfolio> portfolios = new ArrayList<>();

                for (int i = 0; i < jsonArray.size(); i++) {
                    final JsonObject portfolioJson = jsonArray.get(i).getAsJsonObject();
                    final String date = portfolioJson.get("date").getAsString();
                    final String security = portfolioJson.get("security").getAsString();
                    final int quantity = portfolioJson.get("quantity").getAsInt();
                    final String portfolio = portfolioJson.get("portfolio").getAsString();

                    final Portfolio portfolioInfo = new Portfolio(date, security, quantity, portfolio);
                    portfolios.add(portfolioInfo);
                }

                return new PortfolioResponse(page, perPage, totalRecords, nextPage, portfolios);
            }
        }
        
        static class Portfolio {
            String date;
            String security;
            int quantity;
            String portfolio;

            public Portfolio(String date, String security, int quantity, String portfolio) {
                this.date = date;
                this.security = security;
                this.quantity = quantity;
                this.portfolio = portfolio;
            }

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public String getSecurity() {
                return security;
            }

            public void setSecurity(String security) {
                this.security = security;
            }

            public int getQuantity() {
                return quantity;
            }

            public void setQuantity(int quantity) {
                this.quantity = quantity;
            }

            public String getPortfolio() {
                return portfolio;
            }

            public void setPortfolio(String portfolio) {
                this.portfolio = portfolio;
            }
        }
    }
}
