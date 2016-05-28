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

import java.util.Map;

import org.joseki.DatasetDesc;
import org.joseki.Request;
import org.joseki.Response;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rs.AutoReloadableDataset;

/**
 * An copy of de.fuberlin.wiwiss.d2rs.D2RQDatasetDesc to enable usage of
 * il.ac.technion.cs.d2rUpdateServer.AutoReloadableDataset
 * 
 * 
 * @author Vadim Eisenberg <Vadim.Eisenberg@gmail.com>
 * 
 * @see de.fuberlin.wiwiss.d2rs.D2RQDatasetDesc
 */

// TODO check if this class is needed at all, check if
// de.fuberlin.wiwiss.d2rs.D2RQDatasetDesc can be refactored so this class will
// be able to extend it
public class D2RQUpdateDatasetDesc extends DatasetDesc {
	private final AutoReloadableDataset dataset;

	public D2RQUpdateDatasetDesc(AutoReloadableDataset dataset) {
		super(null);
		this.dataset = dataset;
	}



	@Override
	public Dataset acquireDataset(Request request, Response response) {
		dataset.checkMappingFileChanged();
		return this.dataset;
	}

	public void setDefaultGraph(Resource dftGraph) {
		throw new RuntimeException(
				"D2RQUpdateDatasetDecl.setDefaultGraph is not implemented");
	}

	public Resource getDefaultGraph() {
		throw new RuntimeException(
				"D2RQUpdateDatasetDecl.getDefaultGraph is not implemented");
	}

	public void addNamedGraph(String uri, Resource r) {
		throw new RuntimeException(
				"D2RQUpdateDatasetDecl.addNamedGraph is not implemented");
	}

	public Map getNamedGraphs() {
		throw new RuntimeException(
				"D2RQUpdateDatasetDecl.getNamedGraphs is not implemented");
	}

	@Override
	public String toString() {
		return "D2RQUpdateDatasetDecl(" + this.dataset + ")";
	}
}
