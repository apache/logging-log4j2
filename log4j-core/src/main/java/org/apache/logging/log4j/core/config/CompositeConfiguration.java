package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.Filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class CompositeConfiguration
    extends AbstractConfiguration
{

    private List<? extends AbstractConfiguration> configurations;

    private static final List<String> names = new ArrayList<>();

    private static final String APPENDERS = "appenders";

    private static final String PROPERTIES = "properties";

    private static final String LOGGERS = "loggers";


    static
    {
        names.add( APPENDERS );
        names.add( PROPERTIES );
        names.add( LOGGERS );
    }

    /**
     * Constructor.
     */
    public CompositeConfiguration( List<? extends AbstractConfiguration> configurations )
    {
        super( ConfigurationSource.NULL_SOURCE );
        this.configurations = configurations;
    }

    @Override
    protected void setup()
    {
        AbstractConfiguration primaryConfiguration = configurations.get( 0 );
        staffChildConfiguration( primaryConfiguration );
        rootNode = primaryConfiguration.rootNode;
        for ( AbstractConfiguration amendingConfiguration : configurations.subList( 1, configurations.size() ) )
        {
            staffChildConfiguration( amendingConfiguration );
            Node currentRoot = amendingConfiguration.rootNode;
            for ( Node childNode : currentRoot.getChildren() )
            {
                mergeNodes( rootNode, childNode );
            }
        }
    }

    private void staffChildConfiguration( AbstractConfiguration childConfiguration )
    {
        childConfiguration.pluginManager = pluginManager;
        childConfiguration.scriptManager = scriptManager;
        childConfiguration.setup();
    }

    private void mergeNodes( Node rootNode, Node childNode )
    {
        // first find the right rootNode child we will merge with
        boolean isFilter = Filter.class.isAssignableFrom(childNode.getType().getPluginClass());
        for ( Node rootChildNode : rootNode.getChildren() )
        {
            if (isFilter && Filter.class.isAssignableFrom(rootChildNode.getType().getPluginClass())) {
                //for filters we have a simple replace, as only one filter can exist
                rootNode.getChildren().remove(rootChildNode);
                rootNode.getChildren().add(childNode);
                return;
            }
            if ( rootChildNode.getType() != childNode.getType()
                || !names.contains( rootChildNode.getName().toLowerCase() ) )
            {
                continue;
            }

            List<Node> grandChilds = rootChildNode.getChildren();

            switch ( rootChildNode.getName().toLowerCase() )
            {
                case LOGGERS: /** fallthrough */
                case PROPERTIES:
                    grandChilds.addAll( childNode.getChildren() );
                    break;
                case APPENDERS:
                    for ( Node appender : childNode.getChildren() )
                    {
                        Iterator<Node> it = grandChilds.iterator();
                        while ( it.hasNext() )
                        {
                            Node currentAppender = it.next();
                            // check if there's already an appender with the very same name. If so remove it
                            if ( Objects.equals( currentAppender.getAttributes().get( "name" ),
                                appender.getAttributes().get( "name" ) ) )
                            {
                                it.remove();
                                break;
                            }
                        }
                        // add appender (might actually be replace with one with the same name was previously found
                        grandChilds.add( appender );
                    }
                    break;
            }
        }
    }
}
