/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.core.gin;

import java.lang.annotation.Annotation;

import org.jboss.as.console.client.poc.POC;
import org.jboss.errai.ioc.client.container.IOC;

import com.google.inject.Provider;
import com.gwtplatform.mvp.client.proxy.PlaceManager;

public class ErraiPlaceManagerProvider implements Provider<PlaceManager> {

    @Override
    public PlaceManager get() {
        return IOC.getBeanManager().lookupBean(PlaceManager.class, new POC() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return POC.class;
            }
        }).getInstance();
    }

}
