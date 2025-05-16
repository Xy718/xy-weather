package xyz.xy718.demo.weather;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WeatherService {

    @Tool(description = "获取中国指定城市的未来1-5天的天气情况（只可获取市级，如果是省级，请查询省会）")
    public String getWeather(
            @ToolParam(description = "城市的英文名") String city,
            @ToolParam(description = "基于ISO 3166-2标准的省码") String province,
            @ToolParam(description = "天数 范围：[1,5]") String dayNum
    ) {
        // 执行从面试鸭数据库中搜索题目的逻辑（代码省略）
        log.info("city:{},province:{}",city,province);
        return "哈哈";
    }
}
