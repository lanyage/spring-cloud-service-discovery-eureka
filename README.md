1. Eureka服务治理

分为`Eureka Server` `Serive Provider` `Service Consumer`三个角色。

负载均衡

```
@Bean
@LoadBalanced   //负载均衡
RestTemplate restTemplate() {
    return new RestTemplate();
}
```

直接获取服务的内容。

`return restTemplate.getForEntity("http://SERVICE-HELLO/hello", String.class).getBody();`


2.负载均衡`Ribbon`

对于大型应用系统负载均衡(LB:Load Balancing)是首要被解决一个问题。在微服务之前LB方案主要是集中式负载均衡方案，在服务消费者和服务提供者之间又一个独立的LB，LB通常是专门的硬件，如F5，或者是基于软件的，如VS、HAproxy等。LB上有所有服务的地址映射表，当服务消费者调用某个目标服务时，它先向LB发起请求，由LB以某种策略（比如：Round-Robin）做负载均衡后将请求转发到目标服务。

而微服务的出现，则为LB的实现提供了另外一种思路：把LB的功能以库的方式集成到服务消费方的进程内，而不是由一个集中的设备或服务器提供。这种方案称为软负载均衡（Soft Load Balancing）或者客户端负载均衡。在Spring Cloud中配合Eureka的服务注册功能，Ribbon子项目则为REST客户端实现了负载均衡。

开启两个服务提供进程。

```
@RestController
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private EurekaInstanceConfig eurekaInstanceConfig;

    @Value("${server.port}")
    private Integer serverPort;
    @GetMapping(value = "/hello")
    public String hello() {
        logger.info("/hello, InstanceId : {}, host:{}", eurekaInstanceConfig.getInstanceId(), eurekaInstanceConfig.getHostName(false));
        return "Hello, Spring Cloud! My port is " + String.valueOf(serverPort);
    }
}
```

`java -jar xxx.jar --server.port=8082/8083`

然后使用@LoadBalanced注解实现软负载均衡

```
@LoadBalanced   //负载均衡,也就是软负载均衡,该注解能让restTemplate启用客户端负载均衡
RestTemplate restTemplate() {
    return new RestTemplate();
}
```
这样就可以看到
`Hello, Spring Cloud! My port is 8083`和`Hello, Spring Cloud! My port is 8082`交替出现了。从而实现了负载均衡。

同样负载均衡也可以使用`Ribbon`的`API`:`LoadBalancerClient`。

```
@Autowired
private LoadBalancerClient loadBalancerClient;
```

```
@GetMapping("/helloEx")
public String helloEx() {
    ServiceInstance instance = loadBalancerClient.choose("SERVICE-HELLO");
    URI helloUri = URI.create(String.format("http://%s:%s/hello", instance.getHost(), instance.getPort()));
    logger.info("Target service uri = {}", helloUri.toString());
    return new RestTemplate().getForEntity(helloUri, String.class).getBody();
}
``` 
同样可以实现负载均衡。

Ribbon负载均衡策略有几种。

* RoundRobinRule: 轮询策略，Ribbon以轮询的方式选择服务器，这个是默认值。所以示例中所启动的两个服务会被循环访问;
* RandomRule: 随机选择，也就是说Ribbon会随机从服务器列表中选择一个进行访问;
* BestAvailableRule: 最大可用策略，即先过滤出故障服务器后，选择一个当前并发请求数最小的;
* WeightedResponseTimeRule: 带有加权的轮询策略，对各个服务器响应时间进行加权处理，然后在采用轮询的方式来获取相应的服务器;
* AvailabilityFilteringRule: 可用过滤策略，先过滤出故障的或并发请求大于阈值一部分服务实例，然后再以线性轮询的方式从过滤后的实例清单中选出一个;
* ZoneAvoidanceRule: 区域感知策略，先使用主过滤条件（区域负载器，选择最优区域）对所有实例过滤并返回过滤后的实例清单，依次使用次过滤条件列表中的过滤条件对主过滤条件的结果进行过滤，判断最小过滤数（默认1）和最小过滤百分比（默认0），最后对满足条件的服务器则使用RoundRobinRule(轮询方式)选择一个服务器实例。

也可以通过继承`ClientConfigEnabledRoundRobinRule`,来实现自己负载均衡策略。

`Ribbon`主要功能是为了REST客户端实现负载均衡。其工作原理可以概括为下面四个步骤：

1. Ribbon首先根据其所在Zone优先选择一个负载较少的Eureka Server;
2. 定期从Eureka Server更新并过滤服务实例列表;列表存在`Consumer`进程中。
3. 根据指定的负载均衡策略，从可用的服务器列表中选择一个服务实例的地址;
4. 然后通过RestClient进行服务调用。

架构如下：

![](https://upload-images.jianshu.io/upload_images/1488771-4aa4072bff40a204.png?imageMogr2/auto-orient/)


