/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/ 
package org.mmbase.applications.vprowizards.spring.cache;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;

/**
 *This is a cache handler interceptor that implements support for all types of cacheflush hints.
 *
 *
 * @author ebunders
 *
 */
public class BasicCacheHandlerInterceptor extends CacheHandlerInterceptor {

    private static Logger log = Logging.getLoggerInstance(BasicCacheHandlerInterceptor.class);

    
    private CacheNameResolverFactory cacheNameResolverFactory = null;
    private CacheWrapper cacheWrapper = null;

    BasicCacheHandlerInterceptor() {
        //add the namespaces to the cachename resolver

        handlings.add(new Handling(CacheFlushHint.TYPE_REQUEST) {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
                    throws Exception {

                log.debug("handling request type flush hint");
                if (shouldFlush(request)) {
                    TokenizerCacheNameResolver resolver = (TokenizerCacheNameResolver) cacheNameResolverFactory.getCacheNameResolver();
                    resolver.setInput(ServletRequestUtils.getStringParameter(request, "flushname", ""));
                    resolver.setNameSpace("request");
                    flushForName(resolver.getNames());
                }
            }

        });

        handlings.add(new Handling(CacheFlushHint.TYPE_NODE) {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
                    throws Exception {

                log.debug("handling node type flush hint");
                if (shouldFlush(request)) {
                    TokenizerCacheNameResolver resolver = (TokenizerCacheNameResolver) cacheNameResolverFactory.getCacheNameResolver();
                    resolver.setInput(ServletRequestUtils.getStringParameter(request, "flushname", ""));
                    resolver.setNameSpace("node");
                    flushForName(resolver.getNames());
                }
            }
        });

        handlings.add(new Handling(CacheFlushHint.TYPE_RELATION) {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
                    throws Exception {

                log.debug("handling relation type flush hint");
                if (shouldFlush(request)) {
                    TokenizerCacheNameResolver resolver = (TokenizerCacheNameResolver) cacheNameResolverFactory.getCacheNameResolver();
                    resolver.setInput(ServletRequestUtils.getStringParameter(request, "flushname", ""));
                    resolver.setNameSpace("relation");
                    flushForName(resolver.getNames());
                }
            }
        });

    }

    /**
     * flush the given cache groups.
     * @param request
     * @param flushnames a comma separated list of cache groups.
     */
    private void flushForName(List<String> flushnames) {
        for(String name: flushnames) {
            cacheWrapper.flushForName(name);
        }
       
    }

    private boolean shouldFlush(HttpServletRequest request) {
        return ! StringUtils.isEmpty(request.getParameter("flushname"));
    }

   

    public void setCacheNameResolverFactory(CacheNameResolverFactory cacheNameResolverFactory) {
        this.cacheNameResolverFactory = cacheNameResolverFactory;
    }

    public void setCacheWrapper(CacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }


}
