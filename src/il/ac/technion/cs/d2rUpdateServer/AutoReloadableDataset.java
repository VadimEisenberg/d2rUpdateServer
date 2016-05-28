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

import il.ac.technion.cs.d2rqUpdate.D2RQUpdateDatasetGraph;
import il.ac.technion.cs.d2rqUpdate.GraphD2RQUpdate;
import il.ac.technion.cs.d2rqUpdate.ModelD2RQUpdate;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.engine.D2RQDatasetGraph;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rs.D2RServer;

/**
 * An extension to de.fuberlin.wiwiss.d2rs.AutoReloadableDataset to enable usage
 * of ModelD2RQUpdate and GraphD2RQUpdate
 * 
 * 
 * @author Vadim Eisenberg <Vadim.Eisenberg@gmail.com>
 * 
 * @see de.fuberlin.wiwiss.d2rs.AutoReloadableDataset
 */

// TODO refactor this class and de.fuberlin.wiwiss.d2rs.AutoReloadableDataset
// to reduce this class to minimum
public class AutoReloadableDataset extends
		de.fuberlin.wiwiss.d2rs.AutoReloadableDataset {
	private static Log log = LogFactory.getLog(AutoReloadableDataset.class);
	
	/** only reload any this mili seconds */
	private static long RELOAD_FREQUENCY_MS = 1000;

	private final D2RServer server;
	private D2RQDatasetGraph datasetGraph = null;
    
	private final String mappingFile;
	private long lastModified = Long.MAX_VALUE;
	private long lastReload = Long.MIN_VALUE;
	
	/** true if resultSizeLimit is used */
	private boolean hasTruncatedResults;
	
	/** (localFile) => auto-reloadable */
	private final boolean localFile;
	
	private Model defaultModel;
	
	public AutoReloadableDataset(String mappingFile, boolean localFile, D2RServer server) {
		super(mappingFile, localFile, server);
		this.mappingFile = mappingFile;
		this.localFile = localFile;	
		this.server = server;
	}

	/** re-init dsg */
	@Override
	public void forceReload() {
		initD2RQDatasetGraph();		
	}
	
	/** re-init dsg if mapping file has changed */
	@Override
	public void checkMappingFileChanged() {
		if (!localFile || this.mappingFile == null || !server.getConfig().getAutoReloadMapping()) {
			return;
		}
		
		// only reload again if lastReload is older than CHECK_FREQUENCY_MS
		long now = System.currentTimeMillis();
		if (now < this.lastReload + RELOAD_FREQUENCY_MS) {
			return;
		}
		
		long lastmod = new File(this.mappingFile).lastModified();
		if (lastmod == this.lastModified) {
			return;
		}
		
		initD2RQDatasetGraph();
	}
	
	private void initD2RQDatasetGraph() {
		if (this.datasetGraph != null) {
			log.info("Reloading mapping file");
		}
		
		Model mapModel = ModelFactory.createDefaultModel();
		mapModel.read((this.localFile) ? "file:" + this.mappingFile : this.mappingFile, server.resourceBaseURI(), "N3");
		
		this.hasTruncatedResults = mapModel.contains(null, D2RQ.resultSizeLimit, (RDFNode) null);
		ModelD2RQUpdate result =
				new ModelD2RQUpdate(mapModel, server.resourceBaseURI());
		GraphD2RQUpdate graph = (GraphD2RQUpdate) result.getGraph();
		graph.connect();
		graph.initInventory(server.baseURI() + "all/");
		this.datasetGraph = new D2RQUpdateDatasetGraph(graph);
		this.defaultModel = ModelFactory.createModelForGraph(datasetGraph.getDefaultGraph());		
		
		if (localFile) {
			this.lastModified = new File(this.mappingFile).lastModified();
			this.lastReload = System.currentTimeMillis();
		}
	}

	@Override
	public Capabilities getCapabilities() {
		//checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().getCapabilities();
	}

	@Override
	public PrefixMapping getPrefixMapping() {
		//checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().getPrefixMapping();
	}

	@Override
	public boolean hasTruncatedResults() {
		//checkMappingFileChanged();
		return hasTruncatedResults;
	}

	@Override
	public QueryHandler queryHandler() {
		checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().queryHandler();
	}
	
	@Override
	public DatasetGraph asDatasetGraph() {
		// check already done by servlets before getting the graph
		//checkMappingFileChanged();
		return datasetGraph;
	}

	@Override
	public Model getDefaultModel() {
		// check already done earlier, don't care
		//checkMappingFileChanged();
		return defaultModel;
	}

	@Override
	public boolean containsNamedModel(String uri) {
		return false;
	}

	@Override
	public Lock getLock() {
		return datasetGraph.getLock();
	}

	@Override
	public Model getNamedModel(String uri) {
		return null;
	}

	@Override
	public Iterator listNames() {
		return NullIterator.instance();
	}

	@Override
	public void close() {
		datasetGraph.close();
	}
	
}
