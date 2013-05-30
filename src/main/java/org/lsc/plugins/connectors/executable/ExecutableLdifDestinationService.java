/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2011 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.plugins.connectors.executable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.CommunicationException;

import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.TaskType;
import org.lsc.configuration.KeysValuesMap.Entry;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.plugins.connectors.executable.generated.ExecutableLdifDestinationServiceSettings;
import org.lsc.service.IWritableService;
import org.lsc.utils.output.LdifLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a generic but configurable implementation to provision data to
 * any referential which can be scripted
 * 
 * It just requires 6 scripts to :
 * <ul>
 * <li>list data</li>
 * <li>get a piece of data</li>
 * <li>add a new</li>  
 * <li>update a existing data</li>  
 * <li>rename - or change the identifier</li>  
 * <li>delete or archive an unused data</li>  
 * </ul>
 * 
 * The 4 scripts which change data are responsible for consistency. No explicit 
 * check neither rollback is achieved by the LSC engine, so a successful result 
 * for any of these 4 operations must be fully checked.
 * 
 * At this time, no time out is managed. So please consider handling provisioned
 * referential availability and/or time limit handling directly in the executable.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ExecutableLdifDestinationService extends AbstractExecutableLdifService implements IWritableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableLdifDestinationService.class);

	/** Map a JndiModificationType to the associated Script **/
	private Map<JndiModificationType, String> modificationToScript = new HashMap<JndiModificationType, String>();

    private List<String> writableDatasetIds;

	@SuppressWarnings("unchecked")
    public ExecutableLdifDestinationService(TaskType task) throws LscServiceConfigurationException {
        try {
            if (task.getPluginDestinationService().getAny() == null || task.getPluginDestinationService().getAny().size() != 1 || !(task.getPluginDestinationService().getAny().get(0) instanceof ExecutableLdifDestinationServiceSettings)) {
                throw new LscServiceConfigurationException("Unable to identify the executable LDIF destination service configuration " + "inside the plugin destination node of the task: " + task.getName());
            }
            
            ExecutableLdifDestinationServiceSettings serviceSettings = (ExecutableLdifDestinationServiceSettings) task.getPluginDestinationService().getAny().get(0);

            listScript = serviceSettings.getListScript();
            getScript = serviceSettings.getGetScript();
            modificationToScript.put(JndiModificationType.ADD_ENTRY, serviceSettings.getAddScript());
            modificationToScript.put(JndiModificationType.DELETE_ENTRY, serviceSettings.getRemoveScript());
            modificationToScript.put(JndiModificationType.MODIFY_ENTRY, serviceSettings.getUpdateScript());
            modificationToScript.put(JndiModificationType.MODRDN_ENTRY, serviceSettings.getRenameScript());
            
            writableDatasetIds = serviceSettings.getFetchedAttributes().getString();

            globalEnvironmentVariables = new Properties();
            if(serviceSettings.getVariables() != null) {
                for(Entry entry : serviceSettings.getVariables().getEntry()) {
                    globalEnvironmentVariables.put(entry.getKey(), entry.getValue());
                }
            }
            
            interpretor = serviceSettings.getInterpretor();
            interpretorBinary = serviceSettings.getInterpretorBinary();

            beanClass = (Class<IBean>) Class.forName(task.getBean());
        } catch (ClassNotFoundException e) {
            throw new LscServiceConfigurationException(e);
        }
	}

	/**
	 * Apply directory modifications.
	 *
	 * @param lm Modifications to apply in a {@link JndiModifications} object.
	 * @return Operation status
	 * @throws CommunicationException If the connection to the service is lost,
	 * and all other attempts to use this service should fail.
	 */
	public boolean apply(final LscModifications lm) {
		int exitCode = 0;
		String ldif = LdifLayout.format(lm);
		JndiModifications jm = new JndiModifications(JndiModificationType.getFromLscModificationType(lm.getOperation()), lm.getTaskName());
		jm.setDistinguishName(lm.getMainIdentifier());
		jm.setNewDistinguishName(lm.getNewMainIdentifier());
		jm.setModificationItems(JndiModifications.fromLscAttributeModifications(lm.getLscAttributeModifications()));
		exitCode = execute(getParameters(modificationToScript.get(jm.getOperation()), 
						jm.getDistinguishName()), getEnv(), ldif);
		if (exitCode != 0) {
			LOGGER.error("Exit code != 0: {}", exitCode);
		}
		return exitCode == 0;
	}

    @Override
    public List<String> getWriteDatasetIds() {
        return writableDatasetIds;
    }

}
