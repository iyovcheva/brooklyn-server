package brooklyn.location.jclouds;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.location.access.PortForwardManager;
import brooklyn.location.basic.LocationConfigKeys;
import brooklyn.location.cloud.CloudLocationConfig;
import brooklyn.location.jclouds.networking.JcloudsPortForwarderExtension;
import brooklyn.util.internal.ssh.SshTool;
import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import org.jclouds.Constants;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.LoginCredentials;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Semaphore;

public interface JcloudsLocationConfig extends CloudLocationConfig {

    public static final ConfigKey<String> CLOUD_PROVIDER = LocationConfigKeys.CLOUD_PROVIDER;

    public static final ConfigKey<Boolean> RUN_AS_ROOT = ConfigKeys.newBooleanConfigKey("runAsRoot", 
            "Whether to run initial setup as root (default true)", null);
    public static final ConfigKey<String> LOGIN_USER = ConfigKeys.newStringConfigKey("loginUser",
            "Override the user who logs in initially to perform setup " +
            "(otherwise it is detected from the cloud or known defaults in cloud or VM OS)", null);
    public static final ConfigKey<String> LOGIN_USER_PASSWORD = ConfigKeys.newStringConfigKey("loginUser.password",
            "Custom password for the user who logs in initially", null);
    public static final ConfigKey<String> LOGIN_USER_PRIVATE_KEY_DATA = ConfigKeys.newStringConfigKey("loginUser.privateKeyData",
            "Custom private key for the user who logs in initially", null);   
    public static final ConfigKey<String> KEY_PAIR = ConfigKeys.newStringConfigKey("keyPair",
            "Custom keypair name to be re-used", null);
    public static final ConfigKey<Boolean> AUTO_GENERATE_KEYPAIRS = ConfigKeys.newBooleanConfigKey("jclouds.openstack-nova.auto-generate-keypairs",
            "Whether to generate keypairs for Nova");
    public static final ConfigKey<Boolean> AUTO_CREATE_FLOATING_IPS = ConfigKeys.newBooleanConfigKey("jclouds.openstack-nova.auto-create-floating-ips",
            "Whether to generate floating ips for Nova");
    // not supported in jclouds
//    public static final ConfigKey<String> LOGIN_USER_PRIVATE_KEY_PASSPHRASE = ConfigKeys.newStringKey("loginUser.privateKeyPassphrase", 
//            "Passphrase for the custom private key for the user who logs in initially", null);
    public static final ConfigKey<String> LOGIN_USER_PRIVATE_KEY_FILE = ConfigKeys.newStringConfigKey("loginUser.privateKeyFile",
            "Custom private key for the user who logs in initially", null); 
    public static final ConfigKey<String> EXTRA_PUBLIC_KEY_DATA_TO_AUTH = ConfigKeys.newStringConfigKey("extraSshPublicKeyData",
            "Additional public key data to add to authorized_keys", null);
    
    public static final ConfigKey<Boolean> DONT_CREATE_USER = ConfigKeys.newBooleanConfigKey("dontCreateUser", 
            "Whether to skip creation of 'user' when provisioning machines (default false)", false);

    public static final ConfigKey<LoginCredentials> CUSTOM_CREDENTIALS = new BasicConfigKey<LoginCredentials>(LoginCredentials.class, 
            "customCredentials", "Custom jclouds LoginCredentials object to be used to connect to the VM", null);
    
    public static final ConfigKey<String> GROUP_ID = ConfigKeys.newStringConfigKey("groupId",
            "The Jclouds group provisioned machines should be members of. " +
            "Users of this config key are also responsible for configuring security groups.");
    
    // jclouds compatibility
    public static final ConfigKey<String> JCLOUDS_KEY_USERNAME = ConfigKeys.newStringConfigKey(
            "userName", "Equivalent to 'user'; provided for jclouds compatibility", null);
    public static final ConfigKey<String> JCLOUDS_KEY_ENDPOINT = ConfigKeys.newStringConfigKey(
            Constants.PROPERTY_ENDPOINT, "Equivalent to 'endpoint'; provided for jclouds compatibility", null);
    
    // note causing problems on centos due to use of `sudo -n`; but required for default RHEL VM
    public static final ConfigKey<Boolean> OPEN_IPTABLES = ConfigKeys.newBooleanConfigKey("openIptables", 
            "Whether to open the INBOUND_PORTS via iptables rules; " +
            "if true then ssh in to run iptables commands, as part of machine provisioning", false);

    public static final ConfigKey<Boolean> STOP_IPTABLES = ConfigKeys.newBooleanConfigKey("stopIptables", 
            "Whether to stop iptables entirely; " +
            "if true then ssh in to stop the iptables service, as part of machine provisioning", false);

    public static final ConfigKey<Boolean> OS_64_BIT = ConfigKeys.newBooleanConfigKey("os64Bit", 
            "Whether to require 64-bit OS images (true), 32-bit images (false), or either (null)");
    public static final ConfigKey<Integer> MIN_RAM = new BasicConfigKey<Integer>(Integer.class, "minRam",
            "Minimum amount of RAM (in MB), for use in selecting the machine/hardware profile", null);
    public static final ConfigKey<Integer> MIN_CORES = new BasicConfigKey<Integer>(Integer.class, "minCores",
            "Minimum number of cores, for use in selecting the machine/hardware profile", null);
    public static final ConfigKey<Double> MIN_DISK = new BasicConfigKey<Double>(Double.class, "minDisk",
        "Minimum size of disk (in GB), for use in selecting the machine/hardware profile", null);
    public static final ConfigKey<String> HARDWARE_ID = ConfigKeys.newStringConfigKey("hardwareId",
            "A system-specific identifier for the hardware profile or machine type to be used when creating a VM", null);
    
    public static final ConfigKey<String> IMAGE_ID = ConfigKeys.newStringConfigKey("imageId", 
            "A system-specific identifier for the VM image to be used when creating a VM", null);
    public static final ConfigKey<String> IMAGE_NAME_REGEX = ConfigKeys.newStringConfigKey("imageNameRegex", 
            "A regular expression to be compared against the 'name' when selecting the VM image to be used when creating a VM", null);
    public static final ConfigKey<String> IMAGE_DESCRIPTION_REGEX = ConfigKeys.newStringConfigKey("imageDescriptionRegex", 
            "A regular expression to be compared against the 'description' when selecting the VM image to be used when creating a VM", null);

    public static final ConfigKey<String> TEMPLATE_SPEC = ConfigKeys.newStringConfigKey("templateSpec", 
            "A jclouds 'spec' string consisting of properties and values to be used when creating a VM " +
            "(in most cases the properties can, and should, be specified individually using other Brooklyn location config keys)", null);

    public static final ConfigKey<String> DEFAULT_IMAGE_ID = ConfigKeys.newStringConfigKey("defaultImageId", 
            "A system-specific identifier for the VM image to be used by default when creating a VM " +
            "(if no other VM image selection criteria are supplied)", null);

    public static final ConfigKey<TemplateBuilder> TEMPLATE_BUILDER = ConfigKeys.newConfigKey(TemplateBuilder.class,
            "templateBuilder", "A TemplateBuilder instance provided programmatically, to be used when creating a VM");

    public static final ConfigKey<Object> SECURITY_GROUPS = new BasicConfigKey<Object>(Object.class, "securityGroups",
            "Security groups to be applied when creating a VM, on supported clouds " +
            "(either a single group identifier as a String, or an Iterable<String> or String[])", null);

    public static final ConfigKey<String> USER_DATA_UUENCODED = ConfigKeys.newStringConfigKey("userData", 
            "Arbitrary user data, as a uuencoded string, on supported clouds", null);

    public static final ConfigKey<Object> INBOUND_PORTS = new BasicConfigKey<Object>(Object.class, "inboundPorts", 
            "Inbound ports to be applied when creating a VM, on supported clouds " +
            "(either a single port as a String, or an Iterable<Integer> or Integer[])", null);

    public static final ConfigKey<Object> TAGS = new BasicConfigKey<Object>(Object.class, "tags", 
            "Tags to be applied when creating a VM, on supported clouds " +
            "(either a single tag as a String, or an Iterable<String> or String[])", null);

    public static final ConfigKey<Object> USER_METADATA = new BasicConfigKey<Object>(Object.class, "userMetadata", 
            "Arbitrary user metadata, as a map (or String of comma-separated key=value pairs), on supported clouds", null);

    public static final ConfigKey<Boolean> MAP_DEV_RANDOM_TO_DEV_URANDOM = ConfigKeys.newBooleanConfigKey(
            "installDevUrandom", "Map /dev/random to /dev/urandom to prevent halting on insufficient entropy", false);

    public static final ConfigKey<JcloudsLocationCustomizer> JCLOUDS_LOCATION_CUSTOMIZER = new BasicConfigKey<JcloudsLocationCustomizer>(
            JcloudsLocationCustomizer.class, "customizer", "Optional location customizer", null);

    public static final ConfigKey<String> JCLOUDS_LOCATION_CUSTOMIZER_TYPE = ConfigKeys.newStringConfigKey(
            "customizerType", "Optional location customizer type (to be class-loaded and constructed with no-arg constructor)", null);

    @SuppressWarnings("serial")
    public static final ConfigKey<Collection<JcloudsLocationCustomizer>> JCLOUDS_LOCATION_CUSTOMIZERS = ConfigKeys.newConfigKey(
                    new TypeToken<Collection<JcloudsLocationCustomizer>>() {},
                    "customizers", "Optional location customizers", null);

    public static final ConfigKey<File> LOCAL_TEMP_DIR = SshTool.PROP_LOCAL_TEMP_DIR;
    
    public static final ConfigKey<Integer> OVERRIDE_RAM = ConfigKeys.newIntegerConfigKey("overrideRam", "Custom ram value");    
    
    public static final ConfigKey<String> NETWORK_NAME = ConfigKeys.newStringConfigKey(
        "networkName", "Network name to specify as template option (e.g. GCE)");

    /**
     * CUSTOM_MACHINE_SETUP_SCRIPT_URL accepts a URL location that points to a shell script. 
     * Please have a look at locations/jclouds/src/main/resources/sample/script/setup-server.sh as an example
     */
    public static final ConfigKey<String> CUSTOM_MACHINE_SETUP_SCRIPT_URL = ConfigKeys.newStringConfigKey(
            "setup.script", "Custom script to customize a node");
    
    public static final ConfigKey<String> CUSTOM_MACHINE_SETUP_SCRIPT_VARS = ConfigKeys.newStringConfigKey(
            "setup.script.vars", "vars to customize a setup.script i.e.: key1:value1,key2:value2");
    
    public static final ConfigKey<Boolean> GENERATE_HOSTNAME = ConfigKeys.newBooleanConfigKey(
            "generate.hostname", "Use the nodename generated by jclouds", false);

    public static final ConfigKey<Boolean> USE_PORT_FORWARDING = ConfigKeys.newBooleanConfigKey(
            "portforwarding.enabled", "Whether to setup port-forwarding to subsequently access the VM (over the ssh port)", false);
    public static final ConfigKey<JcloudsPortForwarderExtension> PORT_FORWARDER = ConfigKeys.newConfigKey(
            JcloudsPortForwarderExtension.class, "portforwarding.forwarder", "The port-forwarder to use");
    public static final ConfigKey<PortForwardManager> PORT_FORWARDING_MANAGER = BrooklynAccessUtils
            .PORT_FORWARDING_MANAGER;

    public static final ConfigKey<Integer> MACHINE_CREATE_ATTEMPTS = ConfigKeys.newIntegerConfigKey(
            "machineCreateAttempts", "Number of times to retry if jclouds fails to create a VM", 1);

    public static final ConfigKey<Integer> MAX_CONCURRENT_MACHINE_CREATIONS = ConfigKeys.newIntegerConfigKey(
            "maxConcurrentMachineCreations", "Maximum number of concurrent machine-creations", Integer.MAX_VALUE);

    public static final ConfigKey<Semaphore> MACHINE_CREATION_SEMAPHORE = ConfigKeys.newConfigKey(
            Semaphore.class, "machineCreationSemaphore", "Semaphore for controlling concurrent machine creation", null);

    @SuppressWarnings("serial")
    public static final ConfigKey<Function<Iterable<? extends Image>,Image>> IMAGE_CHOOSER = ConfigKeys.newConfigKey(
        new TypeToken<Function<Iterable<? extends Image>,Image>>() {},
        "imageChooser", "An image chooser function to control which images are preferred", 
        new BrooklynImageChooser().chooser());

    // TODO
    
//  "noDefaultSshKeys" - hints that local ssh keys should not be read as defaults
    // this would be useful when we need to indicate a password

}