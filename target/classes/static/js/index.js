const elements = {
    loading: document.getElementById('loading'),
    error: document.getElementById('error'),
    errorText: document.getElementById('errorText'),
    refreshBtn: document.getElementById('refreshBtn'),
    updatedAt: document.getElementById('updatedAt'),
    city: document.getElementById('city'),
    country: document.getElementById('country'),
    weatherText: document.getElementById('weatherText'),
    temperature: document.getElementById('temperature'),
    category: document.getElementById('category'),
    aqi: document.getElementById('aqi'),
    aqiAdvice: document.getElementById('aqiAdvice'),
    pm25: document.getElementById('pm25'),
    so2: document.getElementById('so2'),
    no2: document.getElementById('no2'),
    co: document.getElementById('co'),
    o3: document.getElementById('o3')
};

const chart = echarts.init(document.getElementById('trendChart'));

function setLoading(isLoading) {
    elements.loading.classList.toggle('hidden', !isLoading);
    elements.refreshBtn.disabled = isLoading;
    elements.refreshBtn.innerHTML = isLoading
        ? '<i class="fa-solid fa-rotate fa-spin"></i> 数据加载中'
        : '<i class="fa-solid fa-rotate-right"></i> 刷新数据';
}

function setError(isError, message) {
    elements.error.classList.toggle('hidden', !isError);
    elements.errorText.textContent = message || '数据获取失败，请稍后重试';
}

function valueOrDash(value) {
    return value === null || value === undefined || value === '' ? '--' : value;
}

function renderLocation(location) {
    elements.city.textContent = valueOrDash(location.city);
    elements.country.textContent = valueOrDash(location.country);
}

function renderWeatherNow(weatherNow) {
    elements.weatherText.textContent = valueOrDash(weatherNow.text);
    elements.temperature.textContent = valueOrDash(weatherNow.temp);
}

function getAqiAdvice(aqiValue) {
    const value = Number(aqiValue);
    if (!Number.isFinite(value)) {
        return '等待空气质量数据返回...';
    }
    if (value <= 50) {
        return '空气质量优，适合户外活动';
    }
    if (value <= 100) {
        return '空气质量良，可正常出行';
    }
    if (value <= 150) {
        return '轻度污染，敏感人群减少外出';
    }
    if (value <= 200) {
        return '中度污染，建议佩戴口罩';
    }
    return '污染较重，尽量减少外出';
}

function renderAirQuality(airQuality) {
    elements.category.textContent = valueOrDash(airQuality.category);
    elements.aqi.textContent = valueOrDash(airQuality.aqi);
    elements.aqiAdvice.textContent = getAqiAdvice(airQuality.aqi);
    elements.pm25.textContent = valueOrDash(airQuality.pm25);
    elements.so2.textContent = valueOrDash(airQuality.so2);
    elements.no2.textContent = valueOrDash(airQuality.no2);
    elements.co.textContent = valueOrDash(airQuality.co);
    elements.o3.textContent = valueOrDash(airQuality.o3);
}

function renderChart(forecast) {
    chart.setOption({
        color: ['#f36c3d', '#0b7bd3', '#20b978'],
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(15, 34, 56, 0.92)',
            borderColor: 'rgba(255,255,255,0.12)',
            borderWidth: 1,
            textStyle: {
                color: '#fff'
            },
            axisPointer: {
                type: 'line',
                lineStyle: {
                    color: 'rgba(11, 123, 211, 0.34)'
                }
            }
        },
        legend: {
            top: 0,
            right: 0,
            icon: 'roundRect',
            itemWidth: 18,
            itemHeight: 8,
            textStyle: {
                color: '#51667f',
                fontWeight: 700
            },
            data: ['最高温', '最低温', '湿度']
        },
        grid: {
            left: 44,
            right: 48,
            top: 58,
            bottom: 36
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: forecast.dates || [],
            axisLine: {
                lineStyle: {
                    color: '#b9cee2'
                }
            },
            axisTick: {
                show: false
            },
            axisLabel: {
                color: '#607089'
            }
        },
        yAxis: [
            {
                type: 'value',
                name: '温度 ℃',
                nameTextStyle: {
                    color: '#607089',
                    fontWeight: 700
                },
                splitLine: {
                    lineStyle: {
                        color: 'rgba(96, 112, 137, 0.14)'
                    }
                },
                axisLabel: {
                    color: '#607089'
                }
            },
            {
                type: 'value',
                name: '湿度 %',
                min: 0,
                max: 100,
                nameTextStyle: {
                    color: '#607089',
                    fontWeight: 700
                },
                splitLine: {
                    show: false
                },
                axisLabel: {
                    color: '#607089'
                }
            }
        ],
        series: [
            {
                name: '最高温',
                type: 'line',
                smooth: true,
                symbol: 'circle',
                symbolSize: 8,
                data: forecast.maxTemps || [],
                lineStyle: { width: 4 },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(243, 108, 61, 0.22)' },
                        { offset: 1, color: 'rgba(243, 108, 61, 0.02)' }
                    ])
                }
            },
            {
                name: '最低温',
                type: 'line',
                smooth: true,
                symbol: 'circle',
                symbolSize: 8,
                data: forecast.minTemps || [],
                lineStyle: { width: 4 }
            },
            {
                name: '湿度',
                type: 'line',
                smooth: true,
                yAxisIndex: 1,
                symbol: 'circle',
                symbolSize: 8,
                data: forecast.humidity || [],
                lineStyle: { width: 4 }
            }
        ]
    });
}

function renderDashboard(data) {
    renderLocation(data.location || {});
    renderWeatherNow(data.weatherNow || {});
    renderAirQuality(data.airQuality || {});
    renderChart(data.forecast7 || {});
}

function updateTime() {
    const now = new Date();
    elements.updatedAt.textContent = now.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

async function loadDashboard() {
    setLoading(true);
    setError(false);

    try {
        const locationResponse = await axios.get('/api/location');
        renderLocation(locationResponse.data);

        const results = await Promise.allSettled([
            axios.get('/api/weather/now'),
            axios.get('/api/air/now'),
            axios.get('/api/weather/forecast7')
        ]);

        if (results[0].status === 'fulfilled') {
            renderWeatherNow(results[0].value.data);
        }
        if (results[1].status === 'fulfilled') {
            renderAirQuality(results[1].value.data);
        }
        if (results[2].status === 'fulfilled') {
            renderChart(results[2].value.data);
        }

        const failedResult = results.find(result => result.status === 'rejected');
        if (failedResult) {
            setError(true, failedResult.reason.response?.data?.message || '部分数据加载失败，请稍后重试');
        }

        updateTime();
    } catch (error) {
        setError(true, error.response?.data?.message || '数据获取失败，请稍后重试');
        console.error(error);
    } finally {
        setLoading(false);
    }
}

window.addEventListener('resize', () => chart.resize());
elements.refreshBtn.addEventListener('click', loadDashboard);

renderChart({});
loadDashboard();
