/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.search;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.settings.ModelVersions;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.Preferences;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * GIN provider which provides an index based on the locale and management version.
 *
 * @author Harald Pehl
 */
@ApplicationScoped
public class IndexProvider implements Provider<Index> {

    private final BeanFactory beanFactory;
    private final BootstrapContext bootstrapContext;
    private final ModelVersions modelVersions;

    @Inject
    public IndexProvider(final BeanFactory beanFactory, final BootstrapContext bootstrapContext,
            final ModelVersions modelVersions) {
        this.beanFactory = beanFactory;
        this.bootstrapContext = bootstrapContext;
        this.modelVersions = modelVersions;
    }

    @Produces
    @Override
    public Index get() {
        String operationMode = bootstrapContext.isStandalone() ? "standalone" : "domain";
        String locale = Preferences.get(Preferences.Key.LOCALE) == null
                ? "default"
                : Preferences.get(Preferences.Key.LOCALE);
        String prefix = "org.jboss.as.console.index_" + operationMode + "_" + locale + "_v"
                + modelVersions.get("core-version") + "_";
        return new Index(prefix, beanFactory);
    }
}
