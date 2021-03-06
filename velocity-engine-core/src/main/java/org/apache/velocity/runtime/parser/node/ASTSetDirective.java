package org.apache.velocity.runtime.parser.node;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import org.apache.velocity.app.event.EventHandlerUtil;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.util.introspection.Info;

import java.io.IOException;
import java.io.Writer;

/**
 * Node for the #set directive
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTSetDirective extends SimpleNode
{
    private String leftReference = "";
    private Node right = null;
    private ASTReference left = null;
    private boolean isInitialized;

    /**
     *  This is really immutable after the init, so keep one for this node
     */
    protected Info uberInfo;

    /**
     * Indicates if we are running in strict reference mode.
     */
    protected boolean strictRef = false;

    /**
     * @param id
     */
    public ASTSetDirective(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTSetDirective(Parser p, int id)
    {
        super(p, id);
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#jjtAccept(org.apache.velocity.runtime.parser.node.ParserVisitor, java.lang.Object)
     */
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /**
     *  simple init.  We can get the RHS and LHS as the the tree structure is static
     * @param context
     * @param data
     * @return Init result.
     * @throws TemplateInitException
     */
    public synchronized Object init(InternalContextAdapter context, Object data)
    throws TemplateInitException
    {
        /** This method is synchronized to prevent double initialization or initialization while rendering **/

        if (!isInitialized)
        {
            /*
             *  init the tree correctly
             */
    
            super.init( context, data );
    
            uberInfo = new Info(getTemplateName(),
                    getLine(), getColumn());
    
            right = getRightHandSide();
            left = getLeftHandSide();
    
            strictRef = rsvc.getBoolean(RuntimeConstants.RUNTIME_REFERENCES_STRICT, false);
            
            /*
             *  grab this now.  No need to redo each time
             */
            leftReference = left.firstImage.substring(1);
        
            isInitialized = true;
            
            cleanupParserAndTokens();
        }
            
        return data;
    }

    /**
     *   puts the value of the RHS into the context under the key of the LHS
     * @param context
     * @param writer
     * @return True if rendering was sucessful.
     * @throws IOException
     * @throws MethodInvocationException
     */
    public boolean render( InternalContextAdapter context, Writer writer)
        throws IOException, MethodInvocationException
    {
        /*
         *  get the RHS node, and its value
         */

        Object value = right.value(context);

        if ( value == null && !strictRef)
        {
            String rightReference = null;
            if (right instanceof ASTExpression)
            {
                rightReference = ((ASTExpression) right).lastImage;
            }
            EventHandlerUtil.invalidSetMethod(rsvc, context, leftReference, rightReference, uberInfo);
        }
        
        return left.setValue(context, value);
    }
    
    
    /**
     *  returns the ASTReference that is the LHS of the set statememt
     *  
     *  @return left hand side of #set statement
     */
    private ASTReference getLeftHandSide()
    {
        return (ASTReference) jjtGetChild(0);
    }

    /**
     *  returns the RHS Node of the set statement
     *  
     *  @return right hand side of #set statement
     */
    private Node getRightHandSide()
    {
        return jjtGetChild(1);
    }
}
