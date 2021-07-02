package com.example.sidora.activemq.broker;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.policy.ConstantPendingMessageLimitStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.filter.DestinationMapEntry;
import org.apache.activemq.hooks.SpringContextHook;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.StoreUsage;
import org.apache.activemq.usage.SystemUsage;
import org.apache.activemq.usage.TempUsage;
import org.apache.activemq.util.XBeanByteConverterUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
public class SidoraActivemqBrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SidoraActivemqBrokerApplication.class, args);
    }

    /**
     * To configure an embedded activeMQ server using Spring XML Configuration
     */
    //@Profile("embedded-activemq")
    @Configuration
    @ConditionalOnProperty(name = "spring.activemq.xml.config.enabled")
    @ImportResource("file:${spring.activemq.xmlConfigPath}")
    class EmbeddedActiveMQConfig {

        public EmbeddedActiveMQConfig() {
            log.info("Using Spring XML Configuration");
        }
    }

    /**
     * To configure an embedded activeMQ server using Spring Java Configuration
     */
    @Configuration
    @ConditionalOnProperty(name = "spring.activemq.xml.config.enabled", havingValue = "false")
    @ConfigurationProperties(prefix = "spring.activemq")
    class ActiveMQConfig {

        @NotNull
        private String brokerName;

        @NotNull
        private String data;

        public String getBrokerName() {
            return brokerName;
        }

        public void setBrokerName(String brokerName) {
            this.brokerName = brokerName;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Bean(initMethod = "start", destroyMethod = "stop")
        public BrokerService broker() throws Exception {
            log.info("Using java ActiveMQ configuration");

            final BrokerService broker = new BrokerService();
            broker.setBrokerName(brokerName);
            broker.setDataDirectory(data);
            broker.setPersistent(true);

            MemoryUsage memoryUsage = new MemoryUsage();
            memoryUsage.setPercentOfJvmHeap(70);

            StoreUsage storeUsage = new StoreUsage();
            storeUsage.setLimit(XBeanByteConverterUtil.convertToLongBytes("2 gb"));

            TempUsage tempUsage = new TempUsage();
            tempUsage.setLimit(XBeanByteConverterUtil.convertToLongBytes("1 gb"));

            SystemUsage systemUsage = new SystemUsage();
            systemUsage.setMemoryUsage(memoryUsage);
            systemUsage.setStoreUsage(storeUsage);
            systemUsage.setTempUsage(tempUsage);
            broker.setSystemUsage(systemUsage);

            /*DOS protection, limit concurrent connections to 1000 and frame size to 100MB*/
            TransportConnector transportConnector = new TransportConnector();
            transportConnector.setName("auto");
            transportConnector.setUri(new URI("tcp://0.0.0.0:61616?maximumConnections=1000&amp;wireFormat.maxFrameSize=104857600&amp;daemon=true"));

            List<TransportConnector> transportConnectorList = new ArrayList<>();
            transportConnectorList.add(transportConnector);
            broker.setTransportConnectors(transportConnectorList);

            ConstantPendingMessageLimitStrategy constantPendingMessageLimitStrategy = new ConstantPendingMessageLimitStrategy();
            constantPendingMessageLimitStrategy.setLimit(1000);

            PolicyEntry policyEntry = new PolicyEntry();
            policyEntry.setTopic(">");
            policyEntry.setPendingMessageLimitStrategy(constantPendingMessageLimitStrategy);

            List<DestinationMapEntry> destinationMapEntries = new ArrayList<>();
            destinationMapEntries.add(policyEntry);

            PolicyMap policyMap = new PolicyMap();
            policyMap.setPolicyEntries(destinationMapEntries);

            broker.setDestinationPolicy(policyMap);

            List<Runnable> hooks = new ArrayList<>();
            hooks.add(new SpringContextHook());

            broker.setShutdownHooks(hooks);

            broker.start(true);
            return broker;
        }
    }

}
