package xyz.xy718.demo.weather;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WeatherService {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    ObjectMapper om=new  ObjectMapper();
    @Value("${openweather.apiKey}")
    private String apiKey;

    @Tool(description = "获取中国指定城市的未来1-5天的天气情况（只可获取市级，如果是省级，请查询省会）")
    public String getWeather(
            @ToolParam(description = "城市的英文名") String city,
            @ToolParam(description = "基于ISO 3166-2标准的省码") String province,
            @ToolParam(description = "天数 范围：[1,5]") String dayNum
    ) throws JsonProcessingException {
        // 执行从面试鸭数据库中搜索题目的逻辑（代码省略）
        log.info("city:{},province:{}",city,province);
        if (!StringUtils.hasText(city)||!StringUtils.hasText(province)||!StringUtils.hasText(dayNum)) {
            log.warn("未找到城市信息.");
            throw new Error("必须给我城市码");
        }
        String code=city+","+province+",CN";
        log.info("获取天气：{}", code);
        //获取城市经纬度
        ArrayNode locationData = (ArrayNode) getJSON("http://api.openweathermap.org/geo/1.0/direct?q="+code+"&limit=1&appid="+apiKey);

        if (locationData.get(0).isEmpty()) {
            log.warn("未找到城市信息");
            return "未找到城市信息";
        }

        double lat = locationData.get(0).get("lat").asDouble();
        double lon = locationData.get(0).get("lon").asDouble();
        log.info("城市经纬度：{},{}", lat, lon );
        //获取天气情况
        int cnt=Integer.parseInt(dayNum)*8;
        JsonNode weatherResp = getJSON("http://api.openweathermap.org/data/2.5/forecast?lat="+lat+"&lon="+lon+"&cnt="+cnt+"&appid="+apiKey+"&units=metric&lang=zh_cn");
        if (weatherResp.isEmpty()) {
            log.warn("获取天气数据格式错误");
            return "获取天气数据格式错误";
        }
        ArrayNode weatherData=weatherResp.withArray("list");

        List<Object> weatherList=new ArrayList<>();
        for(int i=0;i<weatherData.size();i++){
            JsonNode crtWeather=weatherData.get(i);
            // let datetime=crtWeather.dt_txt
            String datetime=LocalDateTime.ofInstant(Instant.ofEpochSecond(crtWeather.get("dt").asLong()), ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            double temp=crtWeather.get("main").get("temp").asDouble();
            // let max=crtWeather.main.temp_max
            // let min=crtWeather.main.temp_min
            String weather=crtWeather.withArray("weather").get(0).get("description").asText();
            int visibility=crtWeather.get("visibility").asInt();
            double wind=crtWeather.get("wind").get("speed").asDouble();
            int windDeg=crtWeather.get("wind").get("deg").asInt();
            double windGust=crtWeather.get("wind").get("gust").asDouble();
            String weatherTxt=String.format("%s的气温：%s,天气情况：%s,能见度：%s,风力：%s,角度：%s,阵风：%s;",datetime,temp,weather,visibility,wind,windDeg,windGust);
            weatherList.add(weatherTxt);
        }
        //组合为可读性稍高的数据返回给大模型
        ObjectNode weather = om.createObjectNode();
        weather.put("city", code);
        weather.put("weatherList", om.writeValueAsString(weatherList));
        return weather.toString();
    }

    private JsonNode getJSON(String url){
        try {
            HttpGet httpGet = new HttpGet(url);
            // 3. 执行请求并获取响应
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                // 4. 处理响应状态码
                int statusCode = response.getStatusLine().getStatusCode();
                // 5. 处理响应内容
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseBody = EntityUtils.toString(entity);
                    return om.readValue(responseBody, JsonNode.class);
                }
//                if (statusCode >= 200 && statusCode < 300) {
//                } else {
//                }
                return om.createObjectNode();
            }
        } catch (Exception e) {
            return om.createObjectNode();
        }
    }
}
