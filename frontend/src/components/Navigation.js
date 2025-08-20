import React from 'react';
import { Navbar, Nav } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

const Navigation = () => {
  return (
    <Navbar bg="primary" variant="dark" expand="lg">
      <LinkContainer to="/">
        <Navbar.Brand>Smart Trainingsplan</Navbar.Brand>
      </LinkContainer>
      <Navbar.Toggle aria-controls="basic-navbar-nav" />
      <Navbar.Collapse id="basic-navbar-nav">
        <Nav className="me-auto">
          <LinkContainer to="/competitions">
            <Nav.Link>Wettkämpfe</Nav.Link>
          </LinkContainer>
          <LinkContainer to="/overview">
            <Nav.Link>Trainingsplan Übersicht</Nav.Link>
          </LinkContainer>
          <LinkContainer to="/competitions/new">
            <Nav.Link>Neuer Wettkampf</Nav.Link>
          </LinkContainer>
        </Nav>
      </Navbar.Collapse>
    </Navbar>
  );
};

export default Navigation;