import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import React, { useState } from 'react';



export const UserLogin = () => {

const[form,setform]=useState({
email: "",
password: ""
})

const [error, seterror]=useState({});


const handleOnChange = (e) => {
const{name,value}=e.target
setform((prev)=>({...prev,[name]:value}))
}
const handleOnSubmit=(e)=>{
e.preventDefault()

let newError={}
    if(!form.email){
        newError.email="Email is required"
    }
    if(!form.password){
        newError.password="Password is required"
    }

    if(Object.keys(newError).length>0){
    return seterror(newError)
    }

    console.log(form)
    setform({email:"",password:""})
    seterror({})
    }

    return(
        <>
        <Container className="justify-content-md-center">
          <Row>
            <Form onSubmit={handleOnSubmit} className="formdata"> 
                <Col  md={{ span: 6, offset: 3 }}>
                    <Form.Group className="mb-3" controlId="formBasicEmail" >
                        <Form.Label>Email</Form.Label>
                        <Form.Control type="email" placeholder="Email" name="email" 
                        value={form.email} 
                        onChange={handleOnChange}                     
                        isInvalid={!!error.email}
                        />
                        <Form.Control.Feedback type='invalid'>{error.email}</Form.Control.Feedback>
                        <Form.Text className="text-muted">
                        We'll nevser share your email with anyone else.
                        </Form.Text>
                    </Form.Group>
                </Col>
                <Col  md={{ span: 6, offset: 3 }}>
                    <Form.Group className="mb-3" controlId="formBasicPassword">
                        <Form.Label>Password</Form.Label>
                        <Form.Control type="password" placeholder="Password" name="password" 
                        value={form.password}
                        isInvalid={!!error.password}
                        onChange={handleOnChange}  
                        />
                        <Form.Control.Feedback type='invalid'>{error.password}</Form.Control.Feedback>
                    </Form.Group>
                </Col>
              
            
                <Col  md={{ span: 6, offset: 3 }}>
                    <Button variant="primary" type="submit"  style={{ width: '100%' }}>SIGN IN</Button>
                </Col>
            </Form>
          </Row>
          </Container>
        </>
    )
}