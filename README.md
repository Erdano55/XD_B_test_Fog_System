# 西电B测-雾霾探测系统（Fog Detection System

## 项目简介

雾霾探测系统是一个面向空气质量监测的实验项目，用于帮助用户在出行前快速了解当前位置的天气与空气污染情况。  
项目采用 Java 17 + Spring Boot 构建后端服务，前端使用 HTML、CSS、JavaScript、Axios 和 ECharts 实现数据展示与趋势可视化。  
系统通过公网 IP 获取用户所在城市与经纬度，并结合第三方天气和空气质量 API 展示实时环境数据。  
适用于课程实验提交、Web 应用实践展示，以及作为 Java Web 项目入门案例。

## 功能介绍

- **IP 定位获取城市**：根据用户公网 IP 获取经纬度、城市和国家信息，并在服务端保存最近一次定位结果。
- **实时天气展示**：展示当前天气现象和实时温度。
- **空气质量监测**：展示空气质量等级、AQI、PM2.5、SO2、NO2、CO、O3 等指标。
- **未来 7 日天气趋势图**：使用 ECharts 绘制最高温、最低温和湿度折线图。
- **数据可视化展示**：通过现代化 Web 页面展示定位、天气、空气质量和趋势数据。
- **加载与错误提示**：前端提供加载状态、错误提示和刷新按钮，提升交互体验。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.x
- Spring Web
- RestTemplate
- Lombok
- Maven

### 前端

- HTML
- CSS
- JavaScript
- Axios
- ECharts
- Font Awesome

## 系统架构说明

系统采用前后端分离但同项目部署的轻量架构，前端静态资源放在 Spring Boot 的 `static` 目录下，由后端服务直接托管。

整体流程如下：

1. 用户访问 `http://localhost:8080`，浏览器加载静态前端页面。
2. 前端通过 Axios 调用后端 REST API。
3. 后端根据请求调用 IP 定位、实时天气、空气质量和 7 日天气趋势等第三方接口。
4. 后端对第三方接口数据进行解析、封装和聚合。
5. 前端接收 JSON 数据后渲染城市、天气、空气质量卡片和 ECharts 趋势图。

简化架构：

```text
浏览器前端
  |
  | Axios
  v
Spring Boot 后端 API
  |
  | RestTemplate
  v
第三方定位 / 天气 / 空气质量 API
```

## API 接口说明

| 接口 | 方法 | 作用 |
| --- | --- | --- |
| `/api/location` | GET | 根据公网 IP 获取经纬度、城市、国家等定位信息，并保存到服务端内存变量 |
| `/api/weather/now` | GET | 根据最近一次定位经纬度获取当前天气和温度 |
| `/api/air/now` | GET | 根据最近一次定位经纬度获取空气质量等级、AQI 和污染物指标 |
| `/api/weather/forecast7` | GET | 获取未来 7 日最高温、最低温和湿度趋势数据 |
| `/api/dashboard` | GET | 聚合返回定位、实时天气、空气质量和 7 日趋势数据 |

## 项目运行方式

### 1. 克隆项目

```bash
git clone xxx
cd fog-detection-system
```

如果项目已经在本地，直接进入项目根目录即可：

```bash
cd Haze_Detection_System
```

### 2. 检查环境

确保本机已安装：

- JDK 17
- Maven 3.x

可通过以下命令检查：

```bash
java -version
mvn -version
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

### 4. 访问系统

启动成功后，在浏览器访问：

```text
http://localhost:8080
```

也可以直接测试后端接口：

```text
http://localhost:8080/api/location
http://localhost:8080/api/weather/now
http://localhost:8080/api/air/now
http://localhost:8080/api/weather/forecast7
http://localhost:8080/api/dashboard
```

## 项目目录结构

```text
fog-detection-system
├── pom.xml
├── README.md
├── src
│   └── main
│       ├── java
│       │   └── com/example/fog
│       │       ├── FogDetectionApplication.java
│       │       ├── config
│       │       │   └── RestTemplateConfig.java
│       │       ├── controller
│       │       │   └── DashboardController.java
│       │       ├── dto
│       │       │   ├── AirQualityDTO.java
│       │       │   ├── DashboardDTO.java
│       │       │   ├── Forecast7DTO.java
│       │       │   ├── LocationDTO.java
│       │       │   └── WeatherNowDTO.java
│       │       └── service
│       │           ├── AirQualityService.java
│       │           ├── DashboardService.java
│       │           ├── LocationService.java
│       │           └── WeatherService.java
│       └── resources
│           ├── application.yml
│           └── static
│               ├── index.html
│               ├── css/style.css
│               └── js/index.js
```

## 核心实现说明

### 定位模块

后端通过 `LocationService` 调用 IP 定位接口，根据公网 IP 获取经纬度、城市和国家信息。定位结果会保存到 `DashboardService` 的内存变量中，供天气和空气质量接口复用。

### 天气与空气质量模块

`WeatherService` 和 `AirQualityService` 使用 `RestTemplate` 调用第三方天气接口，并解析实时天气、AQI 和污染物数据。系统对第三方接口异常进行了处理，避免接口失败导致后端直接崩溃。

### 前端可视化模块

前端通过 Axios 请求后端接口，使用卡片展示核心空气质量指标，并使用 ECharts 绘制未来 7 日天气趋势图。页面支持普通浏览器和移动端宽度。

## 项目亮点

- 使用 Spring Boot 实现清晰的 Controller、Service、DTO 分层。
- 无数据库依赖，使用内存变量满足实验中的服务端数据保存要求。
- 接入多个第三方 API，完成定位、天气、空气质量和趋势数据聚合。
- 使用 ECharts 实现趋势图可视化，展示效果直观。
- 前端采用响应式设计，页面美观，适合实验验收展示。
- 对第三方 API 调用失败进行了异常处理，前端具备加载和错误提示。

## 注意事项

- IP 定位依赖公网 IP，定位结果可能存在一定偏差，通常只能达到城市级别。
- 天气和空气质量数据依赖第三方 API，运行时需要保证网络可访问。
- 如果接口调用失败，可查看后端控制台日志定位第三方 API 返回信息。

## 适用场景

- Java Web 课程实验
- Spring Boot 入门项目
- 天气与空气质量可视化展示
- 本科实践项目或课程设计
- 面试项目介绍与 GitHub 展示
