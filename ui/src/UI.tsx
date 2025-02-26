import React from "react"
import {Alert, Button, Container, Grid2 as Grid, Paper} from "@mui/material"
import OpenInNewIcon from "@mui/icons-material/OpenInNew"
import {useTranslate} from "@tolgee/react"
import {closeSnackbar, CustomContentProps, SnackbarContent, SnackbarProvider} from "notistack"
import {OphTypography} from "@opetushallitus/oph-design-system"
import {UiVirkailijaRaamit} from "./UIVirkailijaRaamit"
import {CopyLocalisations} from "./CopyLocalisations"
import {LocalisationOverrides} from "./LocalisationOverrides"
import {UIConfig} from "./types"

interface UIProps {
  uiConfig: UIConfig
}

interface ContentProps {
  uiConfig: UIConfig
}

const Content: React.FC<ContentProps> = ({uiConfig}) => {
  const {t} = useTranslate()
  return (
    <Container maxWidth="xl">
      <Grid container spacing={4} sx={theme => ({mt: theme.spacing(4)})}>
        <Grid size={{sm: 12, md: 7}}>
          <Paper elevation={0} sx={theme => ({p: theme.spacing(4)})}>
            <Grid container spacing={3}>
              <Grid size={12}>
                <OphTypography variant="h2">{t("main-title", "Käännösten hallinta")}</OphTypography>
              </Grid>
              <Grid size={12}>
                <OphTypography
                  variant="body1">{t("general-info-text", "Käännöksiä lisätään, muokataan ja poistetaan Tolgeessa, ulkoisessa käännöstenhallintapalvelussa. Tolgee julkaisee käännöstiedostot testiympäristöön, josta ne kopioidaan muihin ympäristöihin alla olevan kopiointitoiminnon avulla.")}</OphTypography>
              </Grid>
              <Grid size={12}>
                <Button href="https://app.tolgee.io" variant="contained" color="primary"
                        target="_blank" startIcon={<OpenInNewIcon/>}>
                  {t("edit-translations", "Muokkaa käännöksiä")}
                </Button>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
        <Grid size={{sm: 12, md: 5}}>
          <Paper elevation={0} sx={theme => ({p: theme.spacing(4), position: "relative"})}>
            <CopyLocalisations uiConfig={uiConfig}/>
          </Paper>
        </Grid>
        <Grid size={12}>
          <Paper elevation={0} sx={theme => ({p: theme.spacing(4)})}>
            <LocalisationOverrides/>
          </Paper>
        </Grid>
      </Grid>
    </Container>
  )
}

interface AlertProps extends CustomContentProps {
  children: React.ReactNode
}

const SuccessAlert =
  React.forwardRef<HTMLDivElement, AlertProps>(({id, message, ...other}, ref) => {
    return (
      <SnackbarContent ref={ref} role="alert" {...other}>
        <Alert variant="filled" severity="success" onClose={() => closeSnackbar(id)}>{message}</Alert>
      </SnackbarContent>
    )
  })
SuccessAlert.displayName = "SuccessAlert"
const ErrorAlert =
  React.forwardRef<HTMLDivElement, AlertProps>(({id, message, ...other}, ref) => {
    return (
      <SnackbarContent ref={ref} role="alert" {...other}>
        <Alert variant="filled" severity="error" onClose={() => closeSnackbar(id)}>{message}</Alert>
      </SnackbarContent>
    )
  })
ErrorAlert.displayName = "ErrorAlert"

export const UI: React.FC<UIProps> = ({uiConfig}) => {
  return (
    <SnackbarProvider
      maxSnack={5}
      autoHideDuration={10000}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      Components={{
        success: SuccessAlert,
        error: ErrorAlert
      }}>
      <UiVirkailijaRaamit scriptUrl="/virkailija-raamit/apply-raamit.js"/>
      <Content uiConfig={uiConfig}/>
    </SnackbarProvider>
  )
}
