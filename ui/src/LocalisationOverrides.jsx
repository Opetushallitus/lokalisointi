import Typography from "@mui/material/Typography"
import React, {useEffect, useState} from "react"
import {IconButton, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material"
import AddOverride from "./AddOverride"
import {Add, Delete} from "@mui/icons-material"

export default function LocalisationOverrides({uiConfig}) {
  const [overrides, setOverrides] = useState([])
  const [addDialodOpen, setAddDialogOpen] = useState(false)
  const loadOverrides = () => {
    fetch("/lokalisointi/api/v1/override", {
      method: "GET",
      credentials: "same-origin"
    })
      .then(res => res.json())
      .then(res => setOverrides(res))
  }
  useEffect(() => {
    loadOverrides()
  }, [])
  const deleteOverride = (id) => {
    fetch(`/lokalisointi/api/v1/override/${id}`, {
      method: "DELETE",
      credentials: "same-origin"
    })
      .then(res => res.json())
      .then(_ => loadOverrides())
  }
  return (
    <>
      <Typography variant="h4">Käännösten yliajot</Typography>
      <Typography variant="body1">Yliajojen kuvausteksti TODO</Typography>
      <TableContainer component={Paper}>
        <Table sx={{minWidth: 650}} aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Namespace</TableCell>
              <TableCell>Locale</TableCell>
              <TableCell>Key</TableCell>
              <TableCell>Value</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Created by</TableCell>
              <TableCell>Updated</TableCell>
              <TableCell>Updated by</TableCell>
              <TableCell></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {!!overrides && overrides.map((override, i) => (
              <TableRow key={i}>
                <TableCell component="th" scope="row">
                  {override.id}
                </TableCell>
                <TableCell>{override.namespace}</TableCell>
                <TableCell>{override.locale}</TableCell>
                <TableCell>{override.key}</TableCell>
                <TableCell>{override.value}</TableCell>
                <TableCell>{override.created}</TableCell>
                <TableCell>{override.createdBy}</TableCell>
                <TableCell>{override.updated}</TableCell>
                <TableCell>{override.updatedBy}</TableCell>
                <TableCell><IconButton onClick={() => deleteOverride(override.id)}><Delete/></IconButton></TableCell>
              </TableRow>
            ))}
            {!!!overrides && (
              <TableRow>
                <TableCell colSpan={10}>Ei yliajoja</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      <IconButton aria-label="Lisää yliajo" onClick={() => {
        setAddDialogOpen(true)
      }}><Add/></IconButton>
      {addDialodOpen && <AddOverride added={() => {
        setAddDialogOpen(false)
        loadOverrides()
      }} uiConfig={uiConfig}/>}
    </>
  )
}
