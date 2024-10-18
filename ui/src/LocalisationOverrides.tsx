import React, {FC, useCallback, useEffect, useState} from "react"
import Typography from "@mui/material/Typography"
import {
  Dialog,
  DialogTitle,
  DialogActions,
  DialogContent,
  DialogContentText,
  Grid2 as Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip, Button
} from "@mui/material"
import dayjs from "dayjs"
import AddOverride from "./AddOverride"
import {Add, Delete, Person} from "@mui/icons-material"
import {LocalisationOverride, UIConfig} from "./types"

interface Props {
  uiConfig?: UIConfig,
  showMessage: (message: React.ReactNode) => void
}

const LocalisationOverrides: FC<Props> = ({uiConfig, showMessage}) => {
  const [overrides, setOverrides] = useState<LocalisationOverride[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState<boolean>(false)
  const [deleteDialogId, setDeleteDialogId] = useState<number | undefined>(undefined)
  const loadOverrides = useCallback(() => {
    fetch("/lokalisointi/api/v1/override", {
      method: "GET",
      credentials: "same-origin"
    })
      .then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          showMessage(`Yliajojen lataaminen ei onnistunut: ${JSON.stringify(body)}`)
          return
        }
        setOverrides(body)
      })
  }, [showMessage])
  useEffect(() => {
    loadOverrides()
  }, [loadOverrides, showMessage])
  const deleteOverride = (id: number) => {
    fetch(`/lokalisointi/api/v1/override/${id}`, {
      method: "DELETE",
      credentials: "same-origin"
    })
      .then(res => res.json())
      .then(_ => {
        deleteDialogClose()
        loadOverrides()
      })
  }
  const deleteDialogClose = () => {
    setDeleteDialogId(undefined)
  }
  return (
    <Grid container spacing={4}>
      <Grid size={12}>
        <Typography variant="h4">Käännösten yliajot</Typography>
      </Grid>
      <Grid size={12}>
        <Typography variant="body1">Yliajojen kuvausteksti TODO</Typography>
      </Grid>
      <Grid size={12}>
        <TableContainer component={Paper}>
          <Table sx={{minWidth: 650}} aria-label="listaus käännösten yliajoista">
            <TableHead>
              <TableRow>
                <TableCell>tunniste</TableCell>
                <TableCell width="15%">nimiavaruus</TableCell>
                <TableCell width="10%">kieli</TableCell>
                <TableCell width="20%">avain</TableCell>
                <TableCell width="20%">arvo</TableCell>
                <TableCell>luontiaika</TableCell>
                <TableCell>luonut</TableCell>
                <TableCell>päivitysaika</TableCell>
                <TableCell>päivittänyt</TableCell>
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
                  <TableCell>{dayjs(override.created).format("L LT")}</TableCell>
                  <TableCell>
                    <Tooltip title={override.createdBy}>
                      <Person/>
                    </Tooltip>
                  </TableCell>
                  <TableCell>{dayjs(override.updated).format("L LT")}</TableCell>
                  <TableCell>
                    <Tooltip title={override.updatedBy}>
                      <Person/>
                    </Tooltip>
                  </TableCell>
                  <TableCell>
                    <Tooltip title="poista">
                      <IconButton onClick={() => setDeleteDialogId(override.id)}><Delete/></IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {!!!overrides && (
                <TableRow>
                  <TableCell colSpan={10}>Ei yliajoja</TableCell>
                </TableRow>
              )}

              {!addDialogOpen ? (
                <TableRow>
                  <TableCell colSpan={10} align="center">
                    <Tooltip title="lisää uusi">
                      <IconButton onClick={() => {
                        setAddDialogOpen(true)
                      }}><Add/></IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ) : (
                <AddOverride close={() => {
                  setAddDialogOpen(false)
                  loadOverrides()
                }} uiConfig={uiConfig} showMessage={showMessage}/>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Grid>
      <Dialog open={!!deleteDialogId}
              onClose={deleteDialogClose}
              aria-labelledby="delete-dialog-title"
              aria-describedby="delete-dialog-description">
        <DialogTitle id="delete-dialog-title">
          Poista yliajo?
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Haluatko varmasti poistaa käännöksen yliajon?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={deleteDialogClose}>Peruuta</Button>
          <Button onClick={() => deleteOverride(deleteDialogId!)} autoFocus>
            Poista
          </Button>
        </DialogActions>
      </Dialog>
    </Grid>
  )
}

export default LocalisationOverrides