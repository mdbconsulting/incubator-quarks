/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package quarks.samples.connectors.mqtt;

import static quarks.samples.connectors.mqtt.MqttClient.OPT_QOS;
import static quarks.samples.connectors.mqtt.MqttClient.OPT_TOPIC;

import quarks.connectors.mqtt.MqttConfig;
import quarks.connectors.mqtt.MqttStreams;
import quarks.samples.connectors.Options;
import quarks.samples.connectors.Util;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.TopologyProvider;

/**
 * A MQTT subscriber topology application.
 */
public class SubscriberApp {
    private final TopologyProvider tp;
    private final Options options;

    /**
     * @param top the TopologyProvider to use.
     * @param options
     */
    SubscriberApp(TopologyProvider tp, Options options) {
        this.tp = tp;
        this.options = options;
    }
    
    /**
     * Create a topology for the subscriber application.
     */
    public Topology buildAppTopology() {
        Topology t = tp.newTopology("mqttClientSubscriber");

        // Create the MQTT broker connector
        MqttConfig config = Runner.newConfig(options);
        MqttStreams mqtt = new MqttStreams(t, () -> config);
        
        System.out.println("Using MQTT clientId " + config.getClientId());
        
        // Subscribe to the topic and create a stream of messages
        TStream<String> msgs = mqtt.subscribe(options.get(OPT_TOPIC),
                                                options.get(OPT_QOS));
        
        // Process the received msgs - just print them out
        msgs.sink(tuple -> System.out.println(
                String.format("[%s] received: %s", Util.simpleTS(), tuple)));
        
        return t;
    }
    
}
