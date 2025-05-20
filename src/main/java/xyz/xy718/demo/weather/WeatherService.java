package xyz.xy718.demo.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WeatherService {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    ObjectMapper om=new  ObjectMapper();
    @Value("openweather.appKey")
    private String appKey;

    @Tool(description = "获取中国指定城市的未来1-5天的天气情况（只可获取市级，如果是省级，请查询省会）")
    public String getWeather(
            @ToolParam(description = "城市的英文名") String city,
            @ToolParam(description = "基于ISO 3166-2标准的省码") String province,
            @ToolParam(description = "天数 范围：[1,5]") String dayNum
    ) {
        // 执行从面试鸭数据库中搜索题目的逻辑（代码省略）
        log.info("city:{},province:{}",city,province);
        if (!StringUtils.hasText(city)||!StringUtils.hasText(province)||!StringUtils.hasText(dayNum)) {
            throw new Error("必须给我城市码");
            return "必须给我城市码";
        }
        String code=city+","+province+",CN";
        log.info("获取天气：{}", code);
        //获取城市经纬度
        JsonNode locationData = getJSON("http://api.openweathermap.org/geo/1.0/direct?q="+code+"&limit=1&appid="+appKey);

        if (locationData.isEmpty()) {
            return "未找到城市信息";
        }

        double lat = locationData.get("lat").asDouble();
        double lon = locationData.get("lon").asDouble();
        log.info("城市经纬度：{},{}", lat, lon );
        //获取天气情况
        int cnt=Integer.parseInt(dayNum)*8;
        JsonNode weatherData = getJSON("http://api.openweathermap.org/data/2.5/forecast?lat="+lat+"&lon="+lon+"&cnt="+cnt+"&appid="+appKey+"&units=metric&lang=zh_cn");

        if (locationData.isEmpty()) {
            return "'获取天气数据格式错误'";
        }

        List<Object> weatherList=new ArrayList<>();
        for(int i=0;i<weatherData.list.length;i++){
            let crtWeather=weatherData.list[i]
            // let datetime=crtWeather.dt_txt
            let datetime=dt2yyyyMMdd(crtWeather.dt)
            let temp=crtWeather.main.temp
            // let max=crtWeather.main.temp_max
            // let min=crtWeather.main.temp_min
            let weather=crtWeather.weather[0].description
            let visibility=crtWeather.visibility
            let wind=crtWeather.wind.speed
            let windDeg=crtWeather.wind.deg
            let windGust=crtWeather.wind.gust
            let weatherTxt=`${datetime}的气温：${temp},天气情况：${weather},能见度：${visibility},风力：${wind},角度：${windDeg},阵风：${windGust};`
            weatherList.push(weatherTxt)
        }
        //组合为可读性稍高的数据返回给大模型

      const weather = {
                city: code,
                weatherList:weatherList
      };
        return {
                content: [
        {
            type: "text",
                    text: JSON.stringify(weather, null, 2),
        },
        ],
      }
        return "哈哈";
    }

    private JsonNode getJSON(String url){
        try {
            HttpGet httpGet = new HttpGet(url);
            // 3. 执行请求并获取响应
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                // 4. 处理响应状态码
                int statusCode = response.getStatusLine().getStatusCode();
                // 5. 处理响应内容
                if (statusCode >= 200 && statusCode < 300) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String responseBody = EntityUtils.toString(entity);
                        return om.readValue(responseBody, ObjectNode.class);
                    }
                } else {
                }
                return om.createObjectNode();
            }
        } catch (Exception e) {
            return om.createObjectNode();
        }
    }
}
