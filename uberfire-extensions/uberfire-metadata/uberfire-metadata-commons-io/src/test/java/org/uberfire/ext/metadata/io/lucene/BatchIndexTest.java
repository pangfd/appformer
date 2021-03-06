/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.ext.metadata.io.lucene;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;
import org.uberfire.commons.async.DescriptiveThreadFactory;
import org.uberfire.ext.metadata.engine.Observer;
import org.uberfire.ext.metadata.io.BatchIndex;
import org.uberfire.ext.metadata.io.MetadataConfigBuilder;
import org.uberfire.io.IOService;
import org.uberfire.io.attribute.DublinCoreView;
import org.uberfire.io.impl.IOServiceDotFileImpl;
import org.uberfire.java.nio.file.OpenOption;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.attribute.FileAttribute;

import static org.junit.Assert.*;
import static org.uberfire.ext.metadata.io.KObjectUtil.toKCluster;

public class BatchIndexTest extends BaseIndexTest {

    @Override
    protected IOService ioService() {
        if (ioService == null) {
            config = new MetadataConfigBuilder()
                    .withInMemoryMetaModelStore()
                    .useDirectoryBasedIndex()
                    .useInMemoryDirectory()
                    .build();
            ioService = new IOServiceDotFileImpl();
        }
        return ioService;
    }

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{"temp-repo-test"};
    }

    @Test
    public void testIndex() throws IOException, InterruptedException {
        {
            final Path file = ioService().get("git://temp-repo-test/path/to/file.txt");
            ioService().write(file,
                              "some content here",
                              Collections.<OpenOption>emptySet(),
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.author";
                                  }

                                  @Override
                                  public Object value() {
                                      return "My User Name Here";
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.lastModification";
                                  }

                                  @Override
                                  public Object value() {
                                      return new Date();
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.comment";
                                  }

                                  @Override
                                  public Object value() {
                                      return "initial document version, should be revised later.";
                                  }
                              }
            );
        }
        {
            final Path file = ioService().get("git://temp-repo-test/path/to/some/complex/file.txt");
            ioService().write(file,
                              "some other content here",
                              Collections.<OpenOption>emptySet(),
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.author";
                                  }

                                  @Override
                                  public Object value() {
                                      return "My Second User Name";
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.lastModification";
                                  }

                                  @Override
                                  public Object value() {
                                      return new Date();
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.comment";
                                  }

                                  @Override
                                  public Object value() {
                                      return "important document, should be used right now.";
                                  }
                              }
            );
        }
        {
            final Path file = ioService().get("git://temp-repo-test/simple.doc");
            ioService().write(file,
                              "some doc content here",
                              Collections.<OpenOption>emptySet(),
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.author";
                                  }

                                  @Override
                                  public Object value() {
                                      return "My Original User";
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.lastModification";
                                  }

                                  @Override
                                  public Object value() {
                                      return new Date();
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.comment";
                                  }

                                  @Override
                                  public Object value() {
                                      return "unlock document updated, should be checked by boss.";
                                  }
                              }
            );
        }

        {
            final Path file = ioService().get("git://temp-repo-test/xxx/simple.xls");
            ioService().write(file,
                              "plans!?");
        }

        new BatchIndex(config.getIndexEngine(),
                       new Observer() {
                           @Override
                           public void information(final String message) {

                           }

                           @Override
                           public void warning(final String message) {

                           }

                           @Override
                           public void error(final String message) {

                           }
                       },
                       Executors.newCachedThreadPool(new DescriptiveThreadFactory()),
                       indexersFactory(),
                       indexerDispatcherFactory(config.getIndexEngine()),
                       DublinCoreView.class).run(ioService().get("git://temp-repo-test/").getFileSystem(),
                                                 () -> {
                                                     try {
                                                         final String index = toKCluster(ioService().get("git://temp-repo-test/")).getClusterId();

                                                         {

                                                             long hits = config.getIndexProvider().findHitsByQuery(Arrays.asList(index),
                                                                                                                   new MatchAllDocsQuery());

                                                             assertEquals(4,
                                                                          hits);
                                                         }

                                                         {

                                                             TermQuery query = new TermQuery(new Term("dcore.author",
                                                                                                      "name"));

                                                             long hits = config.getIndexProvider().findHitsByQuery(Arrays.asList(index),
                                                                                                                   query);
                                                             assertEquals(2,
                                                                          hits);
                                                         }

                                                         {

                                                             TermQuery query = new TermQuery(new Term("dcore.author",
                                                                                                      "second"));
                                                             long hits = config.getIndexProvider().findHitsByQuery(Arrays.asList(index),
                                                                                                                   query);

                                                             assertEquals(1,
                                                                          hits);
                                                         }

                                                         config.dispose();
                                                     } catch (Exception ex) {
                                                         ex.printStackTrace();
                                                         fail();
                                                     }
                                                 });
    }
}
