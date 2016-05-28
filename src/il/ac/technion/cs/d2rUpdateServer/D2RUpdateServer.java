/*
   Copyright 2010 Technion - Israel Institute of Technology

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
 */
package il.ac.technion.cs.d2rUpdateServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;
import org.joseki.processors.SPARQLUpdate;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rs.D2RServer;


/**
 * An extension to de.fuberlin.wiwiss.d2rs.D2RServer to enable usage of
 * il.ac.technion.cs.d2rUpdateServer.AutoReloadableDataset
 * 
 * 
 * @author Vadim Eisenberg <Vadim.Eisenberg@gmail.com>
 * 
 * @see de.fuberlin.wiwiss.d2rs.D2RServer
 */

// TODO refactor this class and de.fuberlin.wiwiss.d2rs.D2RServer
// to reduce this class to minimum
public class D2RUpdateServer extends D2RServer {
	private final static String SPARQL_SERVICE_NAME = "sparql";
	private final static String SPARQL_UPDATE_SERVICE_NAME = "sparqlupdate";
	private static final Log log = LogFactory.getLog(D2RUpdateServer.class);

	/** the dataset, auto-reloadable in case of local mapping files */
	private AutoReloadableDataset datasetForUpdate;
	private boolean overrideUseAllOptimizationsUpdate;

	/**
	 * @return the auto-reloadable dataset which contains a GraphD2RQ as its
	 *         default graph, no named graphs
	 */
	@Override
	public AutoReloadableDataset dataset() {
		return this.datasetForUpdate;
	}

	/**
	 * @return The graph currently in use; will change to a new instance on
	 *         auto-reload
	 */
	@Override
	public GraphD2RQ currentGraph() {
		if (this.datasetForUpdate == null) {
			return super.currentGraph();
		}
		return (GraphD2RQ) this.datasetForUpdate.asDatasetGraph()
				.getDefaultGraph();
	}

	@Override
	public void start() {
		super.start();
		if (getConfig().isLocalMappingFile()) {
			this.datasetForUpdate =
					new AutoReloadableDataset(getConfig()
							.getLocalMappingFilename(),
							true, this);
		} else {
			this.datasetForUpdate =
					new AutoReloadableDataset(getConfig().getMappingURL(),
							false,
							this);
		}
		this.datasetForUpdate.forceReload();

		if (this.overrideUseAllOptimizationsUpdate) {
			currentGraph().getConfiguration().setUseAllOptimizations(true);
		}

		if (currentGraph().getConfiguration().getUseAllOptimizations()) {
			log.info("Fast mode (all optimizations)");
		} else {
			log
					.info("Safe mode (launch using --fast to use all optimizations)");
		}

		Registry.add(RDFServer.ServiceRegistryName,
				createJosekiServiceRegistry());
	}


	@Override
	public void overrideUseAllOptimizations(boolean overrideAllOptimizations) {
		super.overrideUseAllOptimizations(overrideAllOptimizations);
		this.overrideUseAllOptimizationsUpdate = overrideAllOptimizations;
	}


	@Override
	protected ServiceRegistry createJosekiServiceRegistry() {
		log.debug("D2RUpdateServer:createJosekiServiceRegistry called");
		ServiceRegistry services = new ServiceRegistry();
		Service service =
				new Service(new SPARQL(), D2RUpdateServer.SPARQL_SERVICE_NAME,
						new D2RQUpdateDatasetDesc(datasetForUpdate));
		services.add(D2RUpdateServer.SPARQL_SERVICE_NAME, service);
		service =
				new Service(new SPARQLUpdate(), SPARQL_UPDATE_SERVICE_NAME,
						new D2RQUpdateDatasetDesc(datasetForUpdate));
		services.add(SPARQL_UPDATE_SERVICE_NAME, service);
		return services;
	}
}
