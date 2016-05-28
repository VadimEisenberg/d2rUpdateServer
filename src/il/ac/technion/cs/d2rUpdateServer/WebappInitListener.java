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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import de.fuberlin.wiwiss.d2rs.ConfigLoader;
import de.fuberlin.wiwiss.d2rs.D2RServer;
import de.fuberlin.wiwiss.d2rs.VelocityWrapper;

/**
 * Initialize D2R Update server on startup of an appserver such as Tomcat. This
 * listener should be included in the web.xml. This is compatible with Servlet
 * 2.3 spec compliant appservers.
 * 
 * the only difference with de.fuberlin.wiwiss.d2rs.WebappInitListener is usage
 * of D2RUpdateServer
 * 
 * @author Vadim Eisenberg <Vadim.Eisenberg@gmail.com>
 * @see de.fuberlin.wiwiss.d2rs.WebappInitListener
 */
// TODO refactor this class and de.fuberlin.wiwiss.d2rs.WebappInitListener
// to reduce this class to minimum
public class WebappInitListener extends
		de.fuberlin.wiwiss.d2rs.WebappInitListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();
		D2RServer server = new D2RUpdateServer();
		String configFile = context.getInitParameter("overrideConfigFile");
		if (configFile == null) {
			if (context.getInitParameter("configFile") == null) {
				throw new RuntimeException("No configFile configured in web.xml");
			}
			configFile = absolutize(context.getInitParameter("configFile"), context);
		}
		if (context.getInitParameter("port") != null) {
			server.overridePort(Integer.parseInt(context.getInitParameter("port")));
		}
		if (context.getInitParameter("baseURI") != null) {
			server.overrideBaseURI(context.getInitParameter("baseURI"));
		}
		if (context.getInitParameter("useAllOptimizations") != null) {
			server.overrideUseAllOptimizations(context.getInitParameter("useAllOptimizations").equalsIgnoreCase("true"));
		}
		server.setConfigFile(configFile);
		server.start();
		server.putIntoServletContext(context);
		VelocityWrapper.initEngine(server, context);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing
	}

	private String absolutize(String fileName, ServletContext context) {
		if (!fileName.matches("[a-zA-Z0-9]+:.*")) {
			fileName = context.getRealPath("WEB-INF/" + fileName);
		}
		return ConfigLoader.toAbsoluteURI(fileName);
	}
}
