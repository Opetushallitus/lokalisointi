import React, {FC} from "react"
import {blue, pink, grey} from "@mui/material/colors"
import CssBaseline from "@mui/material/CssBaseline"
import {createTheme, ThemeProvider as MuiThemeProvider} from "@mui/material/styles"
import dayjs from "dayjs"
import LocalizedFormat from "dayjs/plugin/localizedFormat"

require("dayjs/locale/fi")

dayjs.locale("fi")
dayjs.extend(LocalizedFormat)

const theme = createTheme({
  palette: {
    primary: blue,
    secondary: pink,
    text: {
      primary: grey[900],
      secondary: grey[700],
    },
    background: {
      default: "#eee",
      paper: "#fff",
    },
    divider: grey[300],
  },
  spacing: 8,
  typography: {
    fontFamily: [
      "-apple-system",
      "BlinkMacSystemFont",
      '"Segoe UI"',
      "Roboto",
      "Helvetica",
      "Arial",
      "sans-serif",
    ].join(","),
  },
  components: {
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {},
    },
    MuiTextField: {
      defaultProps: {
        variant: "filled",
      },
    },
  },
})

interface Props {
  children: React.ReactNode
}

const ThemeProvider: FC<Props> = ({children}) => {
  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline/>
      {children}
    </MuiThemeProvider>
  )
}

export default ThemeProvider