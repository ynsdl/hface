package org.hface;

import com.hazelcast.client.impl.HazelcastClientProxy;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.instance.HazelcastInstanceImpl;
import com.hazelcast.instance.HazelcastInstanceProxy;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.management.TimedMemberStateFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class InstanceStatsTask implements Callable<String>, Serializable, HazelcastInstanceAware {

    // using hz logger not to depend on a specific logging jar
    private static final ILogger logger = Logger.getLogger( InstanceStatsTask.class );

    private static final long serialVersionUID = 42L;

    private static final String COULD_NOT_CREATE_HZ_INSTANCE = "{:error \"could not create Hazelcast instance\"}";
    private static final String COULD_NOT_COLLECT_STATS = "{:error \"could not collect stats from instance: \"}";

    private transient HazelcastInstance hazelcastInstance;

    public void setHazelcastInstance( HazelcastInstance hazelcastInstance ) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public static HazelcastInstanceImpl field( Object object, String field ) {

        try {
            Field f = object.getClass().getDeclaredField(field);
            f.setAccessible( true );
            return ( HazelcastInstanceImpl ) f.get( object );
        }
        catch ( Exception e ) {

            logger.warning( "could not read field [" + field + "] from an object [" + object + "]" );
            return null; // should have been "Optional", but then it would require another jar
        }
    }

    public static HazelcastInstanceImpl proxyToInstance( HazelcastInstance proxy ) {

        if ( proxy instanceof HazelcastInstanceProxy )
            return field(proxy, "original");

        if ( proxy instanceof HazelcastClientProxy )
            return field( proxy, "client" );

        // do the best effort.. to cast :)
        return ( ( HazelcastInstanceImpl ) proxy );
    }

    public String call() throws Exception {

        HazelcastInstanceImpl instance = proxyToInstance( this.hazelcastInstance );

        try {

            if ( instance != null ) {

                return ( new TimedMemberStateFactory( instance ) )
                        .createTimedMemberState()
                        .toJson()
                        .toString();
            } else {
                return COULD_NOT_CREATE_HZ_INSTANCE;
            }
        }
        catch ( Throwable t ) {
            return COULD_NOT_COLLECT_STATS + instance;
        }
    }
}
